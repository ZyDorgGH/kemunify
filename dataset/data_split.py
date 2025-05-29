# Split dataset with Stratified Split method

from pathlib import Path
import random
import os
import sys
import xml.etree.ElementTree as ET
from sklearn.model_selection import train_test_split

# Set the paths
image_path = '/content/dataset/all'
train_path = '/content/dataset/train'
val_path = '/content/dataset/validation'
test_path = '/content/dataset/test'
labelmap_path = '/content/labelmap.txt'  # Path ke file labelmap

# Baca daftar kelas dari labelmap.txt
with open(labelmap_path, 'r') as f:
    classes = [line.strip() for line in f.readlines()]

# Kumpulkan semua file gambar (sama seperti sebelumnya)
if sys.platform == 'linux':
    file_list = list(Path(image_path).rglob('*.[jJ][pP][gG]')) + \
               list(Path(image_path).rglob('*.[jJ][pP][eE][gG]')) + \
               list(Path(image_path).rglob('*.png')) + \
               list(Path(image_path).rglob('*.bmp'))
else:
    file_list = list(Path(image_path).rglob('*.jpg')) + \
               list(Path(image_path).rglob('*.jpeg')) + \
               list(Path(image_path).rglob('*.png')) + \
               list(Path(image_path).rglob('*.bmp'))

# 1. Ekstrak label utama untuk setiap gambar
image_class_mapping = {}

for img_path in file_list:
    xml_path = img_path.with_suffix('.xml')
    if not xml_path.exists():
        continue

    tree = ET.parse(xml_path)
    root = tree.getroot()
    
    # Cari semua kelas dalam gambar
    found_classes = set()
    for obj in root.findall('object'):
        class_name = obj.find('name').text
        if class_name in classes:
            found_classes.add(class_name)
    
    # Jika ada kelas yang valid, gunakan kelas pertama sebagai representasi
    if found_classes:
        image_class_mapping[img_path] = next(iter(found_classes))

# 2. Kelompokkan gambar berdasarkan kelas
class_images = {cls: [] for cls in classes}
for img_path, cls in image_class_mapping.items():
    class_images[cls].append(img_path)

# 3. Bagi setiap kelas secara stratified
train_set = []
val_set = []
test_set = []

for cls, images in class_images.items():
    if len(images) < 2:  # Skip kelas dengan <2 sampel
        continue
    
    # Bagi 70% train, 30% sementara
    train_temp, temp = train_test_split(images, test_size=0.3, random_state=42)
    # Bagi 30% menjadi val dan test (50-50)
    val, test = train_test_split(temp, test_size=0.5, random_state=42)
    
    train_set.extend(train_temp)
    val_set.extend(val)
    test_set.extend(test)

print(f'Total train: {len(train_set)}')
print(f'Total val: {len(val_set)}')
print(f'Total test: {len(test_set)}')

# 4. Pindahkan file ke folder tujuan
def move_files(file_list, dest_dir):
    for img_path in file_list:
        xml_path = img_path.with_suffix('.xml')
        if not xml_path.exists():
            continue
            
        # Pindahkan gambar
        os.rename(img_path, os.path.join(dest_dir, img_path.name))
        # Pindahkan XML
        os.rename(xml_path, os.path.join(dest_dir, xml_path.name))

# Pastikan folder tujuan ada
os.makedirs(train_path, exist_ok=True)
os.makedirs(val_path, exist_ok=True)
os.makedirs(test_path, exist_ok=True)

move_files(train_set, train_path)
move_files(val_set, val_path)
move_files(test_set, test_path)
