# This script is based on the official Google tutorial for adding metadata to object detection models
# https://ai.google.dev/edge/litert/models/metadata_writer_tutorial#object_detectors
from tflite_support.metadata_writers import object_detector
from tflite_support.metadata_writers import writer_utils

# === Hardcoded paths ===
model_path = "custom_model_lite/model.tflite"
labelmap_path = "labelmap.txt"
output_path = "custom_model_lite/model_metadata.tflite"

# === Normalization constants ===
_INPUT_NORM_MEAN = 127.5
_INPUT_NORM_STD = 127.5

def write_metadata(model_path, labelmap_path, output_path):
    ObjectDetectorWriter = object_detector.MetadataWriter

    # Create metadata writer
    writer = ObjectDetectorWriter.create_for_inference(
        writer_utils.load_file(model_path),
        [_INPUT_NORM_MEAN],
        [_INPUT_NORM_STD],
        [labelmap_path]
    )

    # Print metadata JSON
    print(writer.get_metadata_json())

    # Save model with metadata
    writer_utils.save_file(writer.populate(), output_path)
    print(f"✅ Metadata saved to: {output_path}")

if __name__ == "__main__":
    write_metadata(model_path, labelmap_path, output_path)
