import os
import sys
from subprocess import Popen, PIPE

"""
code generation to create indexing templates for ES
"""
mydir = os.path.dirname(os.path.realpath(sys.argv[0]))
server = "ec2-52-88-7-3.us-west-2.compute.amazonaws.com"


def get_list_of_drug_comps():
    drug_companies = None
    drug_comps_file = open(mydir+"/all_drug_companies.txt","r")
    with drug_comps_file as f:
        drug_companies = f.readlines()
    drug_comps_file.close()
    return drug_companies
    
    
def create_curl_template_files():
    print("create indicies files")
    drug_companies = get_list_of_drug_comps()
    curl_templ_file = open(mydir+"/curl_templ","r")
    curl_templ_str = curl_templ_file.read()
    #print(curl_templ_str)
    
    for drug_company in drug_companies:
        if drug_company.strip()!="":
            drug_company= drug_company.rstrip("\n")
            curl_templ_str2 = curl_templ_str.replace("ZZZ","pharma_"+drug_company)
            curl_file = open(mydir+"/out/pharma_"+drug_company+".curl",'w')
            curl_file.write("curl -X PUT http://"+server+":9200/_template/pharma_"+drug_company+" -d '"+curl_templ_str2+"'"+' -H "Accept: application/json"')
            curl_file.close()
    curl_templ_file.close()


def create_templates_helper(drug_comp=None):
    file_path = mydir+"/out/pharma_"+drug_comp+".curl"
    print(file_path)
    curl_file = open(file_path,'r')
    curl_file_str = curl_file.read()
    #print(curl_file_str)
    sproc = Popen(curl_file_str, stdout=PIPE, shell=True)
    out, err = sproc.communicate()
    if err :
        print(err)
    #print(out)


def create_logstash_template():
    file_path = mydir+"/logstash.curl"
    print(file_path)
    curl_file = open(file_path,'r')
    curl_file_str = curl_file.read()
    print(curl_file_str)
    sproc = Popen(curl_file_str, stdout=PIPE, shell=True)
    out, err = sproc.communicate()
    if err :
        print(err)
    

def create_template(drug_comp=None):
    print("create all templates")
    if(drug_comp!=None):
        create_templates_helper(drug_comp)
    else:
        drug_companies = get_list_of_drug_comps()
        for drug_company in drug_companies:
            if drug_company.strip()!="" and drug_company is not None:
                create_templates_helper(drug_company.rstrip('\n'))


def delete_logstash_template():
    print("delete all indicies")
    curl_cmd = "curl -XDELETE 'http://"+server+":9200/_template/logstash*/'"
    sproc = Popen(curl_cmd, stdout=PIPE, shell=True)
    out, err = sproc.communicate()
    print(out)
    
def delete_templates():
    print("delete all indicies")
    curl_cmd = "curl -XDELETE 'http://"+server+":9200/_template/pharma_*/'"
    sproc = Popen(curl_cmd, stdout=PIPE, shell=True)
    out, err = sproc.communicate()
    print(out)
    

if __name__=="__main__":
    # multi indicies
    #delete_templates()
    #create_curl_template_files()
    #create_template()

    # logstash only
    delete_logstash_template()
    create_logstash_template()
    