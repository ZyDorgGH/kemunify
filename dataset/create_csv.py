# Convert XML annotations (Pascal VOC) to CSV format, adapted from GitHub user datitran.
# https://github.com/datitran/raccoon_dataset/blob/master/xml_to_csv.py

import os
import glob
import pandas as pd
import xml.etree.ElementTree as ET

def convert_xml_to_csv(folder_path):
    xml_list = []
    for xml_file in glob.glob(path + '/*.xml'):
        tree = ET.parse(xml_file)
        root = tree.getroot()

        for obj in root.findall('object'):
            filename = root.find('filename').text
            width = int(root.find('size/width').text)
            height = int(root.find('size/height').text)
            label = obj.find('name').text
            bbox = obj.find('bndbox')
            xmin = int(bbox.find('xmin').text)
            ymin = int(bbox.find('ymin').text)
            xmax = int(bbox.find('xmax').text)
            ymax = int(bbox.find('ymax').text)

            records.append((filename, width, height, label, xmin, ymin, xmax, ymax))

    columns = ['filename', 'width', 'height', 'class', 'xmin', 'ymin', 'xmax', 'ymax']
    return pd.DataFrame(records, columns=columns)

def run_conversion():
    folders = ['train', 'validation']
    base_path = os.path.join(os.getcwd(), 'images')

    for folder in folders:
        folder_path = os.path.join(base_path, folder)
        csv_data = convert_xml_to_csv(folder_path)
        csv_output = os.path.join(base_path, f'{folder}_labels.csv')
        csv_data.to_csv(csv_output, index=False)
        print(f'Data from {folder} folder has been saved to CSV.')

run_conversion()
