# KRecall

# How to compile
## Use conveyor to compile the project.

### Output Test Dir

`./gradlew desktopJar`
For Windows:
`conveyor make windows-app`
For current OS:
`conveyor make app`
More information see: https://conveyor.hydraulic.dev/17.0/tutorial/hare/jvm/#__tabbed_2_2


### Output web
`./gradlew desktopJar`

`conveyor make site`

# Use

## About PPOCR
models文件夹需要
原文件名 -> 新文件名
1. ch_PP-OCRv3_det_infer.onnx -> det_infer.onnx
2. ch_PP-OCRv3_rec_infer.onnx -> rec_infer.onnx
3. ch_ppocr_mobile_v2.0_cls_infer.onnx -> cls_infer.onnx
4. ppocr_keys_v1.txt -> ppocr_keys.txt