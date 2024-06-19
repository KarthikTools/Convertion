import pandas as pd
import uuid
from lxml import etree as ET

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
    root = ET.fromstring(sample_xml_content)
    ns = {'': 'urn:iso:std:iso:20022:tech:xsd:pain.001.001.03'}

    # Update GrpHdr with dynamic values
    grp_hdr = root.find('.//GrpHdr', namespaces=ns)
    grp_hdr.find('MsgId', namespaces=ns).text = generate_unique_id()
    grp_hdr.find('CreDtTm', namespaces=ns).text = "2024-06-19T15:30:00"
    grp_hdr.find('NbOfTxs', namespaces=ns).text = str(sum(len(block['transactions']) for block in payment_blocks))
    grp_hdr.find('CtrlSum', namespaces=ns).text = str(update_control_sum([txn for blk in payment_blocks for txn in blk['transactions']]))

    # Remove existing PmtInf elements if needed
    pmt_inf_parent = root.find('.//CstmrCdtTrfInitn', namespaces=ns)
    for pmt_inf in root.findall('.//PmtInf', namespaces=ns):
        pmt_inf_parent.remove(pmt_inf)

    for block in payment_blocks:
        pymnt_inf = ET.SubElement(pmt_inf_parent, "PmtInf")
        pymnt_inf_id = ET.SubElement(pymnt_inf, "PmtInfId")
        pymnt_inf_id.text = generate_unique_id()
        pmt_mtd = ET.SubElement(pymnt_inf, "PmtMtd")
        pmt_mtd.text = "TRF"
        btch_bookg = ET.SubElement(pymnt_inf, "BtchBookg")
        btch_bookg.text = "false"
        nb_of_txs = ET.SubElement(pymnt_inf, "NbOfTxs")
        nb_of_txs.text = str(len(block['transactions']))
        ctrl_sum = ET.SubElement(pymnt_inf, "CtrlSum")
        ctrl_sum.text = str(update_control_sum(block['transactions']))

        # Add additional elements if needed
        pmt_tp_inf = ET.SubElement(pymnt_inf, "PmtTpInf")
        svc_lvl = ET.SubElement(pmt_tp_inf, "SvcLvl")
        cd = ET.SubElement(svc_lvl, "Cd")
        cd.text = "SEPA"
        lcl_instrm = ET.SubElement(pmt_tp_inf, "LclInstrm")
        lcl_cd = ET.SubElement(lcl_instrm, "Cd")
        lcl_cd.text = "INST"
        reqd_exctn_dt = ET.SubElement(pymnt_inf, "ReqdExctnDt")
        reqd_exctn_dt.text = "2024-06-20"
        dbtr = ET.SubElement(pymnt_inf, "Dbtr")
        dbtr_nm = ET.SubElement(dbtr, "Nm")
        dbtr_nm.text = "Debtor Name"
        dbtr_pstl_adr = ET.SubElement(dbtr, "PstlAdr")
        dbtr_ctry = ET.SubElement(dbtr_pstl_adr, "Ctry")
        dbtr_ctry.text = "CA"
        dbtr_adr_line1 = ET.SubElement(dbtr_pstl_adr, "AdrLine")
        dbtr_adr_line1.text = "123 Debtor St."
        dbtr_adr_line2 = ET.SubElement(dbtr_pstl_adr, "AdrLine")
        dbtr_adr_line2.text = "Debtor City"
        dbtr_id = ET.SubElement(dbtr, "Id")
        dbtr_org_id = ET.SubElement(dbtr_id, "OrgId")
        dbtr_othr = ET.SubElement(dbtr_org_id, "Othr")
        dbtr_id_inner = ET.SubElement(dbtr_othr, "Id")
        dbtr_id_inner.text = "9876543210"
        dbtr_acct = ET.SubElement(pymnt_inf, "DbtrAcct")
        dbtr_acct_id = ET.SubElement(dbtr_acct, "Id")
        dbtr_iban = ET.SubElement(dbtr_acct_id, "IBAN")
        dbtr_iban.text = "CA12345678901234567890"
        dbtr_ccy = ET.SubElement(dbtr_acct, "Ccy")
        dbtr_ccy.text = "CAD"
        dbtr_agt = ET.SubElement(pymnt_inf, "DbtrAgt")
        dbtr_fin_instn_id = ET.SubElement(dbtr_agt, "FinInstnId")
        dbtr_bicfi = ET.SubElement(dbtr_fin_instn_id, "BICFI")
        dbtr_bicfi.text = "DEMOCA21"

        for transaction in block['transactions']:
            cdt_trf_tx_inf = ET.SubElement(pymnt_inf, "CdtTrfTxInf")
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

    tree = ET.ElementTree(ET.fromstring(ET.tostring(root)))
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

    # Extract transactions with validations
    transactions = extract_transactions_with_validations(data_prep_df)

    # Group transactions by payment block
    payment_blocks = {}
    for txn in transactions:
        block = txn['payment_block']
        if block not in payment_blocks:
            payment_blocks[block] = {'transactions': []}
        payment_blocks[block]['transactions'].append(txn)

    # Extract values and XPaths for each downstream system
    downstream_system_values = {
        "Enrolment": extract_values_and_xpath(enrolment_df),
        "CAV": extract_values_and_xpath(cav_df),
        "SPS": extract_values_and_xpath(sps_df),
        "FRAUD": extract_values_and_xpath(fraud_df),
        "SANCTION": extract_values_and_xpath(sanction_df),
        "SPS_REVERSAL": extract_values_and_xpath(sps_reversal_df),
    }

    # Create the PAIN.001 XML with validations
    pain001_xml_tree = create_pain001_xml_with_validations(list(payment_blocks.values()), downstream_system_values, sample_xml_content)

    # Save the generated XML to a file
    pain001_xml_tree.write(output_xml_path, pretty_print=True, xml_declaration=True, encoding="UTF-8")

# Example usage:
# main('sample.xml', 'data.xlsx', 'output.xml')
