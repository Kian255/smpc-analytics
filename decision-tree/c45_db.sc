import shared3p;
import shared3p_random;
import shared3p_string;
import shared3p_sort;
import stdlib;

import oblivious;
import shared3p_oblivious;

import shared3p_table_database;
import table_database;


domain pd_shared3p shared3p;

template <type T>
string itoa(T x){
    return arrayToString(x);
}

template <type T>
pd_shared3p xor_uint8[[1]] itoa(pd_shared3p T x){
    pd_shared3p uint8[[1]] stru(20);
    uint[[1]] div = {10000000000000000000,1000000000000000000,100000000000000000,10000000000000000,1000000000000000,100000000000000,10000000000000,1000000000000,100000000000,10000000000,1000000000,100000000,10000000,1000000,100000,10000,1000,100,10,1};
    stru = (uint8) (((uint64)x / div) % 10);

    // Number of zeroes on the left
    pd_shared3p bool[[1]] mask = stru == 0;
    pd_shared3p uint8 nzero = (uint8) truePrefixLength(mask);

    // Add ASCII offset to non-zero symbols
    pd_shared3p uint8[[1]] idx(20);
    for (uint i = 0; i < 20; ++i) {
        idx[i] = (uint8) i;
    }
    mask = idx < nzero;

    pd_shared3p uint8[[1]] zero(20) = 0;
    pd_shared3p uint8[[1]] offset(20) = 48;
    stru = stru + choose(mask, zero, offset);
    pd_shared3p xor_uint8[[1]] str = reshare(stru);

    // Rotate left so that the zeroes would move to the right
    idx -= nzero;
    pd_shared3p xor_uint8[[2]] mat(20, 2);
    mat[:, 0] = reshare(idx);
    mat[:, 1] = str;
    str = quicksort(mat, 0 :: uint, true)[:, 1];
    uint64 bound = 20;
    pd_shared3p xor_uint8[[1]] zero_string(20) = bl_str("0", bound);

    pd_shared3p bool eqz = (x == 0);
    pd_shared3p uint8[[1]] res = choose(eqz, reshare(zero_string), reshare(str));
    return reshare(res);
}

template <type T>
int64 index_of(T[[1]] arr, T element) {
    for (uint64 i = 0; i < size(arr); i++) {
        if (arr[i] == element) {
            return (int64)i;
        }
    }
    return -1;
}


template <domain D, type T>
D uint64 index_of_max(D T[[1]] arr) {
    D uint64 idx = 0;
    D T max = arr[0];
    for (uint64 i = 1; i < size(arr); i++) {
        D uint64 gt = (uint64)(arr[i] > max);
        max = gt * arr[i] + (1 - gt) * max;
        idx = gt * i + (1-gt) * idx;
    }
    return idx;
}

template <domain D, type T>
D T max_of_second_array_based_on_first(D T[[1]] arr1, T[[1]] arr2) {
    D T max1 = arr1[0];
    D T max2 = arr2[0];
    for (uint64 i = 1; i < size(arr1); i++) {
        D uint64 gt = (uint64)(arr1[i] > max1);
        max1 = gt * arr1[i] + (1 - gt) * max1;
        max2 = gt * arr2[i] + (1 - gt) * max2;
    }
    return max2;
}


template <domain D, type T>
D uint64 index_of(D T[[1]] arr, D T element) {
    D uint64 idx = 0;
    D uint64 cnt = 0;
    D uint64 found = 0;
    for (uint64 i = 1; i < size(arr); i++) {
        D uint64 eq = (uint64)(arr[i] == element);
        cnt += eq;
        idx = (1-found) * (eq * i + (1-eq) * idx) + found * idx;
        found = (uint64)(cnt > 0);
    }
    return idx;
}

template <domain D, type T>
D bool exists(T[[1]] arr, D T element) {
    D uint64 exists = 0;
    for (uint64 i = 0; i < size(arr); i++) {
        D uint64 eq = (uint64)(arr[i] == element);
        exists += eq;
    }
    return exists > 0;
}

template <type T>
bool _exists(T[[1]] arr, T element) {
    for (uint64 i = 0; i < size(arr); i++) {
        if (arr[i] == element) {
            return true;
        }
    }
    return false;
}


pd_shared3p float64 log2(pd_shared3p float64 n) {
    pd_shared3p float64[[1]] base2 = {2};
    pd_shared3p float64[[1]] n_arr = {n};
    pd_shared3p bool neq = (n != 0);
    n_arr[0] += (float64)(1-(uint64)neq);
    return log(n_arr, base2)[0];
}

template <domain D, type T>
D T sum(D T[[2]] arr) {
    uint64[[1]] dims = shape(arr);
    D T sum = 0;
    for (uint64 i = 0; i < dims[0]; i++ ){
        sum += sum(arr[i,:]);
    }
    return sum;
}

// template <domain D, type T>
pd_shared3p int64 sum(uint64 vmap, uint64 len) {
    pd_shared3p int64 sum = 0;
    for (uint64 i = 0 ; i < len ; i++) {
        pd_shared3p int64[[1]] values = tdbVmapGetValue(vmap, "0", i :: uint64);
        sum += sum(values);
    }
    return sum;
}



pd_shared3p bool all_examples_same(uint64 example_indexes_vmap) {
    pd_shared3p uint64 res = 0;
    float64[[1]] possible_classes = tdbVmapGetValue(possible_values, itoa(class_index), 0::uint64);
    pd_shared3p uint64[[1]] class_counts(size(possible_classes));
    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        pd_shared3p uint64 partial_res = 0;
        string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
        pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
        uint64 rows = tdbGetRowCount(datasource, table);
        uint64 columns = tdbGetColumnCount(datasource, table);
        pd_shared3p float64[[1]] label_column = tdbReadColumn(datasource, table, class_index);
        pd_shared3p int64 positive_indexes = sum(example_indexes);
        if (!_exists(categorical_attributes, class_index) ){ // If class attribute is NOT categorical, Compute corresponding cells (integers from floats).
            float64 width = (class_max - class_min) / (float64) class_cells;
            label_column = (float64)((int64)((label_column - class_min) / width));
        }
        for (uint64 a = 0; a < size(possible_classes); a++) {
            float64 label = possible_classes[a];
            pd_shared3p uint64[[1]] eq = (uint64)(label_column == label) * (uint64)(example_indexes != 0);
            class_counts[a] = sum(eq);
            partial_res += (uint64)(class_counts[a] == (uint64) positive_indexes);
        }
        res += (uint64) (partial_res > 0);
    }
    return (res == data_providers_num);
}


pd_shared3p float64 entropy(uint64 example_indexes_vmap) {
    pd_shared3p float64 entropy = 0.0;
    pd_shared3p int64 total_count = 0;
    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
        total_count += sum(example_indexes);
    }
    float64[[1]] possible_classes = tdbVmapGetValue(possible_values, itoa(class_index), 0::uint64);
    for (uint64 c = 0; c < size(possible_classes); c++) {
        pd_shared3p uint64 equal_count = 0;
        float64 label = possible_classes[c];
        for (uint64 i = 0 ; i < data_providers_num ; i++) {
            string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
            pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
            uint64 rows = tdbGetRowCount(datasource, table);
            pd_shared3p float64[[1]] label_column = tdbReadColumn(datasource, table, class_index);
            if (!_exists(categorical_attributes, class_index) ){ // If class attribute is NOT categorical, Compute corresponding cells (integers from floats).
                float64 width = (class_max - class_min) / (float64) class_cells;
                label_column = (float64)((int64)((label_column - class_min) / width));
            }
            pd_shared3p uint64[[1]] eq = (uint64)(label_column == label) * (uint64)(example_indexes != 0);
            equal_count += sum(eq);
        }
        pd_shared3p float64 percentage = (float64)equal_count / (float64)total_count;
        entropy -= (percentage * log2(percentage));
    }
    return entropy;
}


pd_shared3p float64 information_gain(pd_shared3p float64 gain, pd_shared3p int64 total_count, uint64 splitted) {
    uint64 num_of_subsets = tdbVmapValueVectorSize(splitted, "subsets");
    
    for (uint64 i = 0; i < num_of_subsets; i++) {
        pd_shared3p int64 subset_count = 0;
        uint64 subset_vmap = tdbVmapGetValue(splitted, "subsets", i)[0];
        for (uint64 j = 0 ; j < data_providers_num ; j++) {
            pd_shared3p int64[[1]] partial_subset = tdbVmapGetValue(subset_vmap, "0", j);
            subset_count += sum(partial_subset);
        }
        pd_shared3p float64 percentage = (float64)subset_count / (float64)total_count;
        pd_shared3p bool neq = (percentage != 0);
        gain -= (float64)neq * percentage * entropy(subset_vmap);
    }
    return gain;
}


uint64 split_attribute(uint64 example_indexes_vmap, uint64[[1]] attributes) {
    uint64 best_vmap = tdbVmapNew();
    pd_shared3p float64 max_gain = (-1)*(float64)UINT64_MAX;
    pd_shared3p float64 best_threshold = -1;
    uint64 empty_map = tdbVmapNew();
    pd_shared3p uint64 best_splitted = empty_map;
    pd_shared3p uint64 best_attribute = 0;
    uint64 total_rows = 0;
    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
        uint64 rows = tdbGetRowCount(datasource, table);
        total_rows += rows;
    }
    
    pd_shared3p float64 ent = entropy(example_indexes_vmap);
    pd_shared3p int64 total_count = 0;
    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
        total_count += sum(example_indexes);
    }
    
    for (uint64 a = 0; a < size(attributes); a++){
        uint64 attribute = attributes[a];
        if (exists(categorical_attributes, attribute)) { // If attribute is categorical
            uint64 splitted = tdbVmapNew();
            float64[[1]] attribute_values = tdbVmapGetValue(possible_values, itoa(attribute), 0::uint64);
            for (uint64 v = 0; v < size(attribute_values); v++) {
                float64 value = attribute_values[v];
                uint64 subset = tdbVmapNew();
                for (uint64 i = 0 ; i < data_providers_num ; i++) {
                    string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
                    pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
                    uint64 rows = tdbGetRowCount(datasource, table);
                    pd_shared3p float64[[1]] attribute_column(rows) = tdbReadColumn(datasource, table, attribute);
                    pd_shared3p int64[[1]] partial_subset(rows) = (int64)(attribute_column == value) * (int64)(example_indexes != 0);
                    tdbVmapAddValue(subset, "0", partial_subset);
                }
                tdbVmapAddValue(splitted, "subsets", subset);
            }
            pd_shared3p float64 gain = information_gain(ent, total_count, splitted);
            pd_shared3p uint64 gt = (uint64)(gain > max_gain);
            max_gain = (float64)gt * gain + ((float64)(1 - gt)) * max_gain;
            best_attribute = gt * attribute + (1 - gt) * best_attribute;
            best_splitted = gt * splitted + (1 - gt) * best_splitted;
        } else {
            pd_shared3p float64[[2]] combined_array(total_rows, 2);
            pd_shared3p float64[[1]] total_example_indexes(total_rows);
            pd_shared3p float64[[1]] total_attribute_column(total_rows);
            uint64 counter = 0;
            for (uint64 i = 0 ; i < data_providers_num ; i++) {
                string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
                uint64 rows = tdbGetRowCount(datasource, table);
                pd_shared3p float64[[1]] attribute_column(rows) = tdbReadColumn(datasource, table, attribute);
                pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);

                total_example_indexes[counter : counter + rows] = (float64)example_indexes;
                total_attribute_column[counter : counter + rows] = (float64)attribute_column;
                counter += rows;
            }
            combined_array[:,0] = total_example_indexes;
            combined_array[:,1] = total_attribute_column;
            combined_array = sort(combined_array, 1::uint64); // Sort both example_indexes and attribute column, based on attribute column
            pd_shared3p int64 last_found_position = -1;

            for (uint64 i = 0 ; i < total_rows-1 ; i++) { // FIXME Try to find a way to SIMD this.
                pd_shared3p float64 example_index = -1;
                pd_shared3p float64 example_value = -1;
                pd_shared3p float64 next_example_index = -1;
                pd_shared3p float64 next_example_value = -1;
                pd_shared3p uint64 count = 0;
                pd_shared3p float64 found_one = 0;
                pd_shared3p float64 found_two = 0;
                pd_shared3p int64 found_position = -1;

                for (uint64 j = i ; j < total_rows ; j++) {
                    pd_shared3p float64 valid = (float64)(combined_array[j, 0] != 0); //Example is in the subset i.e it is valid.
                    count += (uint64)valid;
                    example_index = (1-found_one) * (valid * combined_array[j, 0] + (1-valid) * example_index) + found_one * example_index;
                    example_value = (1-found_one) * (valid * combined_array[j, 1] + (1-valid) * example_value) + found_one * example_value;
                    found_position = (int64)(1-found_one) * ((int64)valid * (int64)j + (int64)(1-valid) * found_position) + (int64)found_one * found_position;

                    next_example_index = (1-found_two) * (valid * combined_array[j, 0] + (1-valid) * next_example_index) + found_two * next_example_index;
                    next_example_value = (1-found_two) * (valid * combined_array[j, 1] + (1-valid) * next_example_value) + found_two * next_example_value;
                    found_one = (float64)(count > 0);
                    found_two = (float64)(count > 1);
                }
                pd_shared3p float64 neq = (float64) (example_value != next_example_value);

                pd_shared3p float64 new_example = (float64)(found_position != last_found_position);
                last_found_position = found_position;

                pd_shared3p float64 threshold = ((example_value + next_example_value) / 2);
                uint64 splitted = tdbVmapNew();
                uint64 less = tdbVmapNew();
                uint64 greater = tdbVmapNew();
                for (uint64 j = 0 ; j < data_providers_num ; j++) {
                    string table = tdbVmapGetString(providers_vmap, "0", j :: uint64);
                    uint64 rows = tdbGetRowCount(datasource, table);
                    pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", j :: uint64);
                    pd_shared3p float64[[1]] attribute_column(rows) = tdbReadColumn(datasource, table, attribute);
                    pd_shared3p int64[[1]] partial_greater = (int64)(attribute_column > threshold) * (int64)(example_indexes != 0);
                    pd_shared3p int64[[1]] partial_less = (int64)(attribute_column <= threshold) * (int64)(example_indexes != 0);
                    tdbVmapAddValue(less, "0", partial_less);
                    tdbVmapAddValue(greater, "0", partial_greater);
                }
                tdbVmapAddValue(splitted, "subsets", less);
                tdbVmapAddValue(splitted, "subsets", greater);
                pd_shared3p float64 gain = (neq * new_example * found_two) * information_gain(ent, total_count, splitted) + (1 - neq * new_example * found_two) * (-1) * (float64)UINT64_MAX;
                pd_shared3p uint64 gt = (uint64) (gain > max_gain);
                max_gain = (float64)gt * gain + ((float64)(1 - gt)) * max_gain;
                best_threshold = (float64)gt * threshold + (float64)(1 - gt) * best_threshold;
                best_attribute = gt * attribute + (1 - gt) * best_attribute;
                best_splitted = gt * splitted + (1 - gt) * best_splitted;
            }
        }
    }
    tdbVmapAddValue(best_vmap, "best_attribute", declassify(best_attribute));
    tdbVmapAddValue(best_vmap, "best_threshold", declassify(best_threshold));
    tdbVmapAddValue(best_vmap, "best_splitted", declassify(best_splitted));
    return best_vmap;
}


pd_shared3p int64 most_common_label(uint64 example_indexes_vmap) {
    float64[[1]] possible_classes = tdbVmapGetValue(possible_values, itoa(class_index), 0::uint64);
    pd_shared3p uint64[[1]] label_counts(size(possible_classes));
    for (uint64 i = 0 ; i < data_providers_num ; i++) {
        string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
        pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
        pd_shared3p float64[[1]] label_column = tdbReadColumn(datasource, table, class_index);
        if (!_exists(categorical_attributes, class_index) ){ // If class attribute is NOT categorical, Compute corresponding cells (integers from floats).
            float64 width = (class_max - class_min) / (float64) class_cells;
            label_column = (float64)((int64)((label_column - class_min) / width));
        }
        for (uint64 a = 0; a < size(possible_classes); a++) {
            float64 label = possible_classes[a];
            pd_shared3p uint64[[1]] eq = (uint64)((int64)label_column == (int64) label)* (uint64)(example_indexes != 0);
            label_counts[a] += sum(eq);
        }
    }
    return (int64)max_of_second_array_based_on_first(label_counts, (uint64)possible_classes);
}


string c45(uint64 example_indexes_vmap, uint64[[1]] attributes) {
    if (declassify(all_examples_same(example_indexes_vmap))) {
        pd_shared3p int64 label_count = 0;
        pd_shared3p int64 total_count = 0;
        for (uint64 i = 0 ; i < data_providers_num ; i++) {
            string table = tdbVmapGetString(providers_vmap, "0", i :: uint64);
            pd_shared3p int64[[1]] example_indexes = tdbVmapGetValue(example_indexes_vmap, "0", i :: uint64);
            pd_shared3p float64[[1]] label_column = tdbReadColumn(datasource, table, class_index);
            if (!_exists(categorical_attributes, class_index) ){ // If class attribute is NOT categorical, Compute corresponding cells (integers from floats).
                float64 width = (class_max - class_min) / (float64) class_cells;
                label_column = (float64)((int64)((label_column - class_min) / width));
            }
            pd_shared3p float64[[1]] true_labels = (float64)example_indexes * label_column;
            label_count += (int64)sum(true_labels);
            total_count += sum(example_indexes);
        }
        pd_shared3p float64 label = (float64) ((uint64)label_count / (uint64)total_count);
        return itoa(declassify(label));
    }
    if (size(attributes) == 0) {
        return itoa(declassify(most_common_label(example_indexes_vmap)));
    }
    uint64 best_vmap = split_attribute(example_indexes_vmap, attributes);
    uint64 best_attribute = tdbVmapGetValue(best_vmap, "best_attribute", 0::uint64)[0];
    float64 best_threshold = tdbVmapGetValue(best_vmap, "best_threshold", 0::uint64)[0];
    uint64 best_splitted = tdbVmapGetValue(best_vmap, "best_splitted",0::uint64)[0];
    uint64 best_attribute_original_index = index_of(original_attributes, best_attribute); // maybe 1 for loop

    int64 best_attribute_index = index_of(attributes, best_attribute);
    uint64[[1]] first_half = attributes[:(uint64)best_attribute_index];
    uint64[[1]] second_half = attributes[(uint64)best_attribute_index + 1:];
    uint64[[1]] new_attribs = cat(first_half, second_half);

    string branches = "";
    if (exists(categorical_attributes, best_attribute)) {
        float64[[1]] best_attribute_values = tdbVmapGetValue(possible_values, itoa(best_attribute_original_index), 0::uint64);

        uint64 length = size(best_attribute_values);
        for (uint64 v = 0 ; v < length ; v++) {
            float64 value = best_attribute_values[v];
            string branch = "\"" + itoa(best_attribute) + " == " + itoa(value) + "\"" + ": ";

            uint64 subset_vmap = tdbVmapGetValue(best_splitted, "subsets", v)[0];
            if (declassify(sum(subset_vmap, data_providers_num) == 0)) {
                branch += itoa(declassify(most_common_label(example_indexes_vmap)));
            } else {
                branch += c45(subset_vmap, new_attribs);
            }
            branches += branch;
            if (v != length -1) {
                branches += ", ";
            }
        }
    } else{
        string branch = "\"" + itoa(best_attribute)+ " <= " + itoa(best_threshold) + "\"" + ": ";
        uint64 less = tdbVmapGetValue(best_splitted, "subsets", 0::uint64)[0];
        if(declassify(sum(less, data_providers_num) == 0)){
            branch += itoa(declassify(most_common_label(example_indexes_vmap)));
        } else {
            branch += c45(less, new_attribs);
        }
        branches += branch;

        branches += ", ";

        branch = "\"" + itoa(best_attribute) + " > " + itoa(best_threshold) + "\"" + ": ";
        uint64 greater = tdbVmapGetValue(best_splitted, "subsets", 1::uint64)[0];
        if(declassify(sum(greater, data_providers_num) == 0)){
            branch += itoa(declassify(most_common_label(example_indexes_vmap)));
        } else {
            branch += c45(greater, (uint64)new_attribs);
        }
        branches += branch;
    }
    return "{ " + branches + "}";
}


string datasource;
uint64[[1]] categorical_attributes;
float64 class_min;
float64 class_max;
int64 class_cells;



uint64 providers_vmap;
uint64 data_providers_num;
uint64 class_index;

uint64[[1]] original_attributes;
uint64 possible_values;

uint64 rows;
uint64 columns;

