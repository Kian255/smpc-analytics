import ast
import sys
import json
from huepy import *
import os.path
import argparse
import pandas as pd

indentation = '    '

imports = '''
import shared3p;
import shared3p_random;
import shared3p_string;
import shared3p_sort;
import stdlib;

import oblivious;
import shared3p_oblivious;

import shared3p_table_database;
import table_database;
import id3_db;

import create_possible_values;
'''
main_f = '''void main(){
'''


def is_number(s):
    try:
        float(s)
        return True
    except ValueError:
        return False

def quote(x):
    if is_number(x):
        return str(x)
    else:
        return '"' + x + '"'

def main():
    global main_f

    parser = argparse.ArgumentParser()
    parser.add_argument('configuration', help = 'Configuration file of the request')
    parser.add_argument('--DNS', help = 'File with the Hospitals names and IPS.', default = './web/MHMDdns.json')
    parser.add_argument('--summary', help = 'File with the Hospitals names and IPS.', default = './datasets/analysis_test_data/cvi_summary.csv')
    args = parser.parse_args()

    print(run('Generating main..'))

    uid = args.configuration.split('_')[-1].split('.')[0]
    configuration = json.load(open(args.configuration))

    if 'datasources' in configuration:
        numberOfDatasets = len(configuration['datasources'])
        data_providers = '\n'.join([indentation + "string table_" + str(i) + " = " + quote(configuration['datasources'][i] + '_' + uid) + ";" for i in range(len(configuration['datasources']))])
    else:
        dns = json.load(open(args.DNS))
        available_datasources = dns.keys()
        numberOfDatasets = len(available_datasources)
        data_providers = '\n'.join([indentation + "string table_" + str(i) + " = " + quote(available_datasources[i] + '_' + uid) + ";" for i in range(len(available_datasources))])

    main_f += '''
    quote = bl_str("\\"");
    comma = bl_str(", ");
    eq_str = bl_str(" == ");
    space = bl_str(" ");
    colon = bl_str(": ");
    left_curly_br = bl_str("{ ");
    right_curly_br = bl_str("}");

'''
    main_f += data_providers
    main_f += '''
    // Create the data-providers list
    providers_vmap = tdbVmapNew();
    data_providers_num = ''' + str(numberOfDatasets) + ''';
'''
    for i in range(numberOfDatasets):
        main_f += '''
    tdbVmapAddString(providers_vmap, "0", table_'''+ str(i) +''');
'''
    attributesFromPOST = configuration['attributes']
    attributes = [str(a['name']) for a in attributesFromPOST]


    class_attribute = configuration['class_attribute']['name']
    original_attributes = list(range(len(attributes)+1))
    imported_cells = [a['cells'] for a in attributesFromPOST] + [configuration['class_attribute']['cells']]
    main_f += '''
    original_attributes = {'''+','.join(map(str,original_attributes))+'''};
    pd_shared3p uint64[[1]] original_attributes_without_class = {'''+','.join(map(str,original_attributes[:-1]))+'''};
    class_index = ''' + str(len(attributes)) + ''';
'''
    attribute_values = [int(a['cells']) for a in attributesFromPOST]
    columns = len(attributes) + 1
    possible_classes = list(range(int(configuration['class_attribute']['cells'])))
    max_attribute_values = max(max(attribute_values), len(possible_classes))
    possible_values = [possible_value + [-1]*(max_attribute_values-len(possible_value)) for possible_value in [list(range(p)) for p in attribute_values]]
    possible_values.append(possible_classes + [-1]*(max_attribute_values-len(possible_classes))  )
    main_f += '''
    columns = ''' + str(columns) + ''';
    max_attribute_values = ''' + str(max_attribute_values) + ''';

    datasource = "DS1";
    categorical_attributes = {-1};
'''


    mins = []
    maxs = []
    summary = pd.read_csv(args.summary, sep = ',')
    for attribute in attributes+[class_attribute]:
        if attribute in summary['Field'].values:
            mins.append(summary[summary['Field']==attribute][' Min'].item())
            maxs.append(summary[summary['Field']==attribute][' Max'].item())
        else:
            mins.append(0.0)
            maxs.append(0.0)
    main_f += '''
    imported_mins =''' + '{'+ ', '.join([str(x) for x in mins]) +'}' + ''';
    imported_maxs =''' + '{'+ ', '.join([str(x) for x in maxs]) +'}' + ''';

    imported_cells =  {'''+','.join(map(str,imported_cells))+'''};
    print("Opening connection to db: ", datasource);
    tdbOpenConnection(datasource);

    possible_values = reshape({'''+','.join([','.join(map(str,possible_value)) for possible_value in possible_values])+'''},columns,max_attribute_values);
'''

    main_f += '''

    uint64 original_example_indexes_vmap = tdbVmapNew();
    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
        uint64 rows = tdbGetRowCount(datasource, table);
        pd_shared3p int64[[1]] original_example_indexes(rows);
        original_example_indexes = 1;
        tdbVmapAddValue(original_example_indexes_vmap, "0", original_example_indexes);
    }
'''

    main_f += '''
    print("Running ID3 ...");
    pd_shared3p xor_uint8[[1]] root = id3(original_example_indexes_vmap, original_attributes_without_class);
    print(bl_strDeclassify(root));

    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
        // Check if a table exists
        if (tdbTableExists(datasource, table)) {
          // Delete existing table
          print("Deleting table: ", table);
          tdbTableDelete(datasource, table);
        }
    }
}'''

    if os.path.isdir("./ID3/"):
        OUTPUT_DIR = './ID3/'
    elif os.path.isdir("../ID3/"):
        OUTPUT_DIR = '../ID3/'
    else:
        OUTPUT_DIR = './'
    with open(OUTPUT_DIR + 'main_' + uid + '.sc', 'w') as output:
        output.write(imports)
        output.write(main_f)
    print(good('Main generated at ' + OUTPUT_DIR + 'main_' + uid + '.sc'))


if __name__ == '__main__':
    main()