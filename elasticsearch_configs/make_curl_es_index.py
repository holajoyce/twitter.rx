import os
import sys


"""
this script create curl files for elasticsearch
"""
def create_curl_index_file():
    mydir = os.path.dirname(os.path.realpath(sys.argv[0]))
    
    drug_comps_file = open(mydir+"/all_drug_companies.txt","r")
    
    with drug_comps_file as f:
        drug_companies = f.readlines()
        
    curl_templ_file = open(mydir+"/curl_templ","r")
    curl_templ_str = curl_templ_file.read()
    print(curl_templ_str)
    
    for drug_company in drug_companies:
        drug_company= drug_company.rstrip("\n")
        curl_templ_str2 = curl_templ_str.replace("ZZZ",drug_company)
        curl_file = open(mydir+"/out/"+drug_company+".curl",'w')
        curl_file.write(curl_templ_str2)
        curl_file.close()
        
    
    curl_templ_file.close()
    drug_comps_file.close()
    


if __name__=="__main__":
    create_curl_index_file()