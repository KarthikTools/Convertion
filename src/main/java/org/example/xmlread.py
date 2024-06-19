import pandas as pd
import xml.etree.ElementTree as ET
import uuid
from lxml import etree
import os

# Load the sample XML from a file
def load_sample_xml(file_path):
    with open(file_path, 'r') as file:
        return file.read()

# Extract transactions with downstream system validations
def extract_transactions_with_validations(data_prep_df):
    transactions = []
    for index, row in data_prep_df.iterrows():
        if pd.notna(row["Payment Block"]):
            transactions.append({
                "amount": float(row["Transaction"]) if "Transaction" in row else 0.0,
                "payment_block": int(row["Payment Block"]),
                "transaction": int(row["Transaction"]),
                "payment_block_type": row["Payment Block Type"],
                "remittance_info": row.get("RemittanceInfo", ""),
                "validations": {
                    "Enrolment": row["Enrolment"] if "Enrolment" in row else "",
                    "CAV": row["CAV"] if "CAV" in row else "",
                    "SPS": row["SPS"] if "SPS" in row else "",
                    "FRAUD": row["FRAUD"] if "FRAUD" in row else "",
                    "SANCTION": row["SANCTION"] if "SANCTION" in row else "",
                    "SPS_REVERSAL": row["SPS_REVERSAL"] if "SPS_REVERSAL" in row else ""
                }
            })
    return transactions

# Extract values and XPath from the downstream system sheets
def extract_values_and_xpath(sheet_df):
    values_and_xpath = {}
    for index, row in sheet_df.iterrows():
        values_and_xpath[row['Scenario']] = {
            "StubValue": row['StubValue'],
            "XPath": row['X_Path']
        }
    return values_and_xpath

def generate_unique_id():
    return str(uuid.uuid4())

def update_control_sum(transactions):
    return sum(transaction['amount'] for transaction in transactions)

def create_pain001_xml_with_validations(payment_blocks, downstream_system_values, sample_xml_content):
    root = etree.fromstring(sample_xml_content)
    ns = {'': 'urn:iso:std:iso:20022:tech:xsd:pain.001.001.03'}

    # Update GrpHdr with dynamic values
    grp_hdr = root.find('.//GrpHdr', namespaces=ns)
    grp_hdr.find('MsgId', namespaces=ns).text = generate_unique_id()
    grp_hdr.find('CreDtTm', namespaces=ns).text = "2024-06-19T15:30:00"
    grp_hdr.find('NbOfTxs', namespaces=ns).text = str(len(payment_blocks))
    grp_hdr.find('CtrlSum', namespaces=ns).text = str(update_control_sum([txn for blk in payment_blocks for txn in blk['transactions']]))

    # Find the PmtInf element
    pmt_inf = root.find('.//PmtInf', namespaces=ns)
    pmt_inf.find('PmtInfId', namespaces=ns).text = generate_unique_id()

    for block in payment_blocks:
        for transaction in block['transactions']:
            cdt_trf_tx_inf = ET.SubElement(pmt_inf, "CdtTrfTxInf")
            pmt_id = ET.SubElement(cdt_trf_tx_inf, "PmtId")
            end_to_end_id = ET.SubElement(pmt_id, "EndToEndId")
            end_to_end_id.text = generate_unique_id()
            inst_id = ET.SubElement(pmt_id, "InstrId")
            inst_id.text = generate_unique_id()

            # Check Payment Block Type
            if transaction["payment_block_type"] == "fixed debit":
                # Add EqvAmt and XchgRate tags
                eqv_amt = ET.SubElement(cdt_trf_tx_inf, "EqvAmt")
                instd_amt = ET.SubElement(eqv_amt, "InstdAmt", Ccy="USD")
                instd_amt.text = str(transaction['amount'])
                xchg_rate = ET.SubElement(eqv_amt, "XchgRate")
                xchg_rate.text = "1.2"  # Example exchange rate, adjust as necessary
            else:
                # Add InstdAmt tag
                amt = ET.SubElement(cdt_trf_tx_inf, "Amt")
                instd_amt = ET.SubElement(amt, "InstdAmt", Ccy="CAD")
                instd_amt.text = str(transaction['amount'])

            # Add remittance information
            rmt_inf = ET.SubElement(cdt_trf_tx_inf, "RmtInf")
            ustrd = ET.SubElement(rmt_inf, "Ustrd")
            ustrd.text = transaction.get("remittance_info", "")

            # Update transaction values based on downstream system validations
            for system, validation in transaction['validations'].items():
                if validation in downstream_system_values[system]:
                    stub_value = downstream_system_values[system][validation]["StubValue"]
                    xpath = downstream_system_values[system][validation]["XPath"]
                    element = root.xpath(xpath, namespaces=ns)
                    if element:
                        element[0].text = stub_value

    tree = ET.ElementTree(ET.fromstring(etree.tostring(root)))
    return tree

def main(sample_xml_path, excel_file_path, output_xml_path):
    # Load the sample XML
    sample_xml_content = load_sample_xml(sample_xml_path)

    # Load the Excel file
    excel_data = pd.ExcelFile(excel_file_path)

    # Load the DataPrep sheet
    data_prep_df = excel_data.parse('DataPrep', header=3)

    # Load the downstream system sheets
    enrolment_df = excel_data.parse('Enrolment')
    cav_df = excel_data.parse('CAV')
    sps_df = excel_data.parse('SPS')
    fraud_df = excel_data.parse('FRAUD')
    sanction_df = excel_data.parse('SANCTION')
    sps_reversal_df = excel_data.parse('SPS_REVERSAL')

    # Extract the transactions with validations
    transactions_with_validations = extract_transactions_with_validations(data_prep_df)

    # Extract values and XPath from the downstream system sheets
    enrolment_values = extract_values_and_xpath(enrolment_df)
    cav_values = extract_values_and_xpath(cav_df)
    sps_values = extract_values_and_xpath(sps_df)
    fraud_values = extract_values_and_xpath(fraud_df)
    sanction_values = extract_values_and_xpath(sanction_df)
    sps_reversal_values = extract_values_and_xpath(sps_reversal_df)

    # Combine all values and XPath into a single dictionary
    downstream_system_values = {
        "Enrolment": enrolment_values,
        "CAV": cav_values,
        "SPS": sps_values,
        "FRAUD": fraud_values,
        "SANCTION": sanction_values,
        "SPS_REVERSAL": sps_reversal_values
    }

    # Group transactions by payment block
    payment_blocks_with_remittance = {}
    for txn in transactions_with_validations:
        block_id = txn["payment_block"]
        if block_id not in payment_blocks_with_remittance:
            payment_blocks_with_remittance[block_id] = {"transactions": []}
        payment_blocks_with_remittance[block_id]["transactions"].append(txn)

    # Convert the payment blocks dictionary to a list
    payment_blocks_list_with_validations = [block for block in payment_blocks_with_remittance.values()]

    # Generate the Pain001 XML using the extracted data with validations
    pain001_tree_with_validations = create_pain001_xml_with_validations(payment_blocks_list_with_validations, downstream_system_values, sample_xml_content)
    xml_str_with_validations = ET.tostring(pain001_tree_with_validations.getroot(), encoding='utf-8').decode('utf-8')

    # Save the updated XML to the specified output path
    with open(output_xml_path, 'w') as output_file:
        output_file.write(xml_str_with_validations)

    print(f"Generated XML saved to {output_xml_path}")

# Define file paths
sample_xml_path = 'C:\\path\\to\\your\\sample.xml'
excel_file_path = 'C:\\path\\to\\your\\excel\\file.xlsx'
output_xml_path = 'C:\\path\\to\\your\\output\\pain001.xml'

# Run the main function
if __name__ == "__main__":
    main(sample_xml_path, excel_file_path, output_xml_path)
