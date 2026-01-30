package com.example.opencv.webwhiteboard;

import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridge for providing localization data from native Android to the WebView
 * layer.
 */
public class WebLanguageBridge {
    private final String languageCode;
    private final Map<String, Map<String, String>> translationsByLang;

    public WebLanguageBridge(@NonNull String languageCode) {
        this.languageCode = languageCode;
        this.translationsByLang = buildTranslations();
    }

    @JavascriptInterface
    public String getLanguage() {
        return languageCode;
    }

    @JavascriptInterface
    public String getTranslations() {
        Map<String, String> translations = translationsByLang.get(languageCode);
        if (translations == null || translations.isEmpty()) {
            return "{}";
        }
        try {
            return new JSONObject(translations).toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Map<String, String> translations = translationsByLang.get(languageCode);
        if (translations == null || translations.isEmpty()) {
            return text;
        }

        // ========== 核心优化：先匹配完整句（优先返回） ==========
        if (translations.containsKey(text)) {
            return translations.get(text);
        }

        // ========== 兜底：无完整句时，仅替换短关键词（跳过完整句key） ==========
        String result = text;
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            // 跳过完整句（长度>10 或 包含标点，避免重复替换）
            if (key.length() > 10 || key.contains("。") || key.contains(".") || key.contains("：")) {
                continue;
            }
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            result = result.replace(key, value);
        }
        return result;
    }

    private Map<String, Map<String, String>> buildTranslations() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("en", buildEnglishMap());
        return map;
    }

    private Map<String, String> buildEnglishMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        // ========== 新增：完整句优先（放在最顶部） ==========
        // 弹窗核心完整句（解决你的问题的关键）
        map.put("没有找到包含对象的图层。", "No layers with objects were found.");
        map.put("没有找到包含对象的Layer.", "No layers with objects were found.");
        // 其他完整句（统一放这里，保证优先级）
        map.put("扫描图层上没有需要处理的图片。", "No images on the scan layer.");
        map.put("切割图层上没有对象。", "No objects on the cutting layer.");
        map.put("该Layer没有对象.", "No objects in this layer");
        map.put("请先选择一个图层", "Please select a layer first");
        map.put("保存失败", "Save failed");
        map.put("生成G代码失败", "Failed to generate G-code");
        map.put("已保存", "Saved");
        map.put("坐标系统：白板坐标 (左下角为原点，X向右，Y向上)",
                "Coordinate system: Whiteboard coordinates (origin at the bottom left corner, X to the right, Y upward)");
        map.put("注意：拖拽时实际坐标会实时更新，此处仅做显示换算",
                "Note: The actual coordinates will update in real-time when dragging; this is for display conversion only.");
        map.put("选择对象所属的图层，不同图层可以设置不同的打印参数。",
                "Select the layer to which the object belongs; different layers can have different print settings.");
        map.put("图层属性在创建后不可修改", "Layer properties cannot be modified after creation");
        map.put("保存修改", "Save Changes");
        map.put("删除项目", "Delete Project");
        map.put("扫描时的最大激光功率", "Maximum laser powerd during scanning");
        map.put("扫描时的最小激光功率", "Minimum laser power during scanning");
        map.put("孔半径", "Hole rdadius");
        map.put("水平边距", "Horizontal margin");
        map.put("垂直边距", "Vertical margin");
        map.put("直角边长", "Length of the leg");
        map.put("边长", "Side length");
        map.put("添加图层", "Add Layer");
        map.put("孔数量", "Number of holes");
        map.put("扫描图层", "Scan Layer");
        map.put("反向移动偏移:", "Reverse Move Offset:");
        map.put("孔距中心距离", "Hole center distance");
        map.put("对齐到画布", "Align to Canvas");
        map.put("半调模式","Halftone Mode");
        map.put("Floyd-Steinberg 误差扩散", "Floyd-Steinberg Error Diffusion");
        map.put("AM 网点半调", "AM Network Half Adjustment");
        map.put("描边宽度", "Stroke Width");

        // 半调网屏相关
        map.put("图像处理模式","Halftone mode");
        map.put("半调网屏 (AM网点)","AM Halftone");
        map.put("当前为灰度模式 (根据亮度调整激光功率)","Currently in greyscale mode (adjusts laser power based on brightness)");
        map.put("使用网点大小模拟灰度 (适合印刷风格)","Simulate greyscale using dot size (suitable for printed styles)");
        map.put("使用像素抖动模拟灰度 (适合照片还原)","Simulate greyscale using pixel dithering (suitable for photographic reproduction)");
        map.put("1bit抖动 (误差扩散)","1bit dithering");


        // ========== 原有短关键词（保持原有顺序，移到完整句之后） ==========
        // Shape names and basic tools
        map.put("矩形", "Rectangle");
        map.put("L型支架", "L Bracket");
        map.put("U型槽", "U Channel");
        map.put("圆形", "Circle");
        map.put("圆环", "Ring");
        map.put("法兰", "Flange");
        map.put("绘图", "Sketch");
        map.put("手绘", "Freehand");
        map.put("文本", "Text");
        map.put("图片", "Image");
        map.put("直线", "Line");
        map.put("多段线", "Polyline");
        map.put("折线", "Polyline");
        map.put("圆弧", "Arc");
        map.put("扇形", "Sector");
        map.put("圆环", "Ring");
        map.put("等边三角形", "Equilateral Triangle");
        map.put("直角等腰三角形", "Right Isosceles Triangle");
        map.put("等腰直角三角形", "Isosceles Right Triangle");
        map.put("带孔圆板", "Circle Plate with Holes");
        map.put("带孔圆形", "Circle with Holes");
        map.put("带孔矩形板", "Rectangle Plate with Holes");
        map.put("带孔矩形", "Rectangle with Holes");
        map.put("组合", "Group");
        map.put("形状", "Shapes");
        map.put("零件库", "Part Library");
        map.put("导入", "Import");
        map.put("工具栏", "Toolbar");
        map.put("撤销", "Back");
        map.put("清空", "Clear");
        map.put("保存", "Save");
        map.put("保存成功", "Saved successfully");
        map.put("下一步", "Next");
        map.put("返回", "Back");
        map.put("导出预览", "Export Preview");
        map.put("生成", "Generate");
        map.put("生成G代码", "Generate");
        map.put("GenerateG代码", "Generate G-code");
        map.put("G代码", "G-code");
        map.put("生成G代码失败", "Failed to generate G-code");
        map.put("生成G代码...", "Generating G-code...");
        map.put("正在生成G代码...", "Generating G-code...");
        map.put("正在生成合并G代码...", "Generating merged G-code...");
        map.put("正在生成切割G代码...", "Generating cutting G-code...");
        map.put("正在生成平台扫描G代码...", "Generating scan G-code...");
        map.put("正在保存文件...", "Saving file...");
        map.put("正在导入矢量文件", "Importing vector file");
        map.put("正在解析数据...", "Parsing data...");
        map.put("正在加载图片...", "Loading image...");
        map.put("正在解析文件...", "Parsing file...");
        map.put("正在解析路径", "Parsing path");
        map.put("正在处理图层", "Processing layer");
        map.put("正在生成合并G代码", "Generating merged G-code");
        map.put("正在生成切割G代码", "Generating cutting G-code");
        map.put("正在生成平台扫描G代码", "Generating scan G-code");
        map.put("正在生成G代码", "Generating G-code");
        map.put("正在生成切割G代码...", "Generating cutting G-code...");
        map.put("正在生成平台扫描G代码...", "Generating scan G-code...");
        map.put("正在生成合并G代码...", "Generating merged G-code...");
        map.put("正在生成G代码", "Generating G-code");
        map.put("编辑图层属性", "Edit Layer Properties");
        map.put("图层名称:", "Layer Name:");
        map.put("打印方式:", "Printing method:");
        map.put("功率 (%):", "Power (%) :");
        map.put("切割图层", "Cut Layer");
        map.put("颜色", "Color");

        // Toolbar labels
        map.put("选择", "Select");
        map.put("涂鸦", "Draw");
        map.put("橡皮擦", "Eraser");
        map.put("属性", "Properties");
        map.put("图层可见", "Layer Visible");
        map.put("图层", "Layers");
        map.put("基础图形", "Basic shapes");
        map.put("Layer列表", "Layer List");
        map.put("Layer预览", "Layer Preview");
        map.put("设置", "Settings");
        map.put("Layer设置", "Layer Settings");
        map.put("ScanLayer 设置", "ScanLayer Settings");
        map.put("CutLayer 设置", "CutLayer Settings");
        map.put("图片", "Image");
        map.put("导入", "Import");
        map.put("下一步", "Next");
        map.put("工具栏", "Toolbar");
        map.put("擦除范围：", "Eraser Size:");
        map.put("属性编辑", "Properties");
        map.put("图层管理", "Layer Manager");
        map.put("导出预览", "Export Preview");

        // Layer modal and settings
        map.put("创建新图层", "Create Layer");
        map.put("图层名称：", "Layer Name:");
        map.put("图层", "Layer");
        map.put("打印方式：", "Printing Mode:");
        map.put("扫描", "Scan");
        map.put("切割", "Cut");
        map.put("扫描参数：", "Scan Parameters:");
        map.put("线密度 (线/毫米)", "Line Density (lines/mm)");
        map.put("线密度 (线/毫米)：", "Line Density (lines/mm):");
        map.put("线密度", "Line Density");
        map.put("启用半色调", "Enable Halftone");
        map.put("半调网屏", "Halftone");
        map.put("出边距离：", "Overscan Distance:");
        map.put("出边距离 (mm)", "Overscan Distance (mm)");
        map.put("出边距离", "Overscan Distance");
        map.put("每行Scan时在两端额外移动的距离", "Additional travel at both ends on each scan");
        map.put("功率最大值 (%)：", "Max Power (%):");
        map.put("功率最大值 (%)", "Max Power (%)");
        map.put("Scan时的最大激光功率", "Maximum laser power during scan");
        map.put("功率最小值 (%)：", "Min Power (%):");
        map.put("功率最小值 (%)", "Min Power (%)");
        map.put("Scan时的最小激光功率", "Minimum laser power during scan");
        map.put("移动速度 (mm/s)：", "Move Speed (mm/s):");
        map.put("移动速度 (mm/s)", "Move Speed (mm/s)");
        map.put("移动速度", "Move Speed");
        map.put("激光头移动速度", "Laser head move speed");
        map.put("激光头", "Laser head ");
        map.put("激光功率", "Laser power");
        map.put("轨迹列表", "Trajectory List");
        map.put("通用参数：", "Common Parameters:");
        map.put("功率 (%)：", "Power (%):");
        map.put("取消", "Cancel");
        map.put("创建图层", "Create Layer");
        map.put("删除", "Delete");
        map.put("重命名", "Rename");

        // Shapes configuration
        map.put("宽度", "Width");
        map.put("高度", "Height");
        map.put("半径", "Radius");
        map.put("厚度", "Thickness");
        map.put("凸缘", "Flange");
        map.put("外径", "Outer Diameter");
        map.put("内径", "Inner Diameter");
        map.put("螺栓孔中心圆直径", "Bolt Circle Diameter");
        map.put("螺栓孔数量", "Bolt Hole Count");
        map.put("螺栓孔直径", "Bolt Hole Diameter");
        map.put("长度", "Length");
        map.put("段1长度", "Segment 1 Length");
        map.put("段2长度", "Segment 2 Length");
        map.put("段3长度", "Segment 3 Length");
        map.put("拐角角度", "Corner Angle");
        map.put("起始角度", "Start Angle");
        map.put("扫描角度", "Sweep Angle");
        map.put("外圈半径", "Outer Radius");
        map.put("内圈半径", "Inner Radius");
        map.put("角度", "Angle");
        map.put("半径：", "Radius:");

        // Alerts and dialogs
        map.put("文件名：", "File Name:");
        map.put("激光雕刻项目", "Laser Engraving Project");
        map.put("文件将以 .nc 格式Save", "File will be saved as .nc");
        map.put("合并所有Layer的G-code为一个文件", "Merge all layer G-code into one file");
        map.put("生成", "Generate");
        map.put("撤销操作", "Undo Action");
        map.put("导出", "Export");
        map.put("图层管理", "Layer Manager");
        map.put("属性", "Properties");
        map.put("导入", "Import");

        // Camera & gallery
        map.put("拍照", "Capture");
        map.put("取消", "Cancel");
        map.put("请使用HTTPS协议访问此页面，或者使用\"从相册选择\"功能。", "Please use HTTPS or choose from album.");
        map.put("从相册选择", "Choose from Album");
        map.put("您的浏览器不支持摄像头功能，请使用现代浏览器或尝试\"从相册选择\"功能。",
                "Your browser does not support the camera. Please use a modern browser or choose from album.");
        map.put("无法访问摄像头", "Unable to access camera");
        map.put("摄像头权限被拒绝，请在浏览器设置中允许摄像头访问", "Camera permission denied. Please enable it in browser settings.");
        map.put("未找到摄像头设备", "Camera not found");
        map.put("您的设备不支持摄像头功能", "Camera not supported on this device");
        map.put("摄像头被其他应用占用，请关闭其他使用摄像头的应用", "Camera in use by another app. Please close other apps.");
        map.put("由于安全限制，摄像头访问被拒绝。请确保使用HTTPS协议访问此页面。", "Camera access denied due to security. Please use HTTPS.");

        // Home page / image editor
        map.put("图片编辑", "Image Editing");
        map.put("Image编辑", "Image Editing");
        map.put("选择图片", "Select Image");
        map.put("从相册选择或拍摄照片", "Choose from album or take a photo");
        map.put("灰度", "Grayscale");
        map.put("二值化", "Binarize");
        map.put("反色", "Invert");
        map.put("高斯模糊", "Gaussian Blur");
        map.put("线框提取", "Outline Extraction");
        map.put("旋转", "Rotate");
        map.put("裁剪", "Crop");
        map.put("水平翻转", "Flip Horizontal");
        map.put("垂直翻转", "Flip Vertical");
        map.put("撤销操作", "Undo");
        map.put("Undo操作", "Undo action");
        map.put("亮度", "Brightness");
        map.put("对比度", "Contrast");
        map.put("下一步", "Next");
        map.put("确定", "OK");
        map.put("加载中...", "Loading...");
        map.put("操作失败", "Operation Failed");
        map.put("重试", "Retry");
        map.put("取消", "Cancel");
        map.put("无图片数据", "No Image Data");
        map.put("请从首页选择图片后再进行裁剪", "Please select an image before cropping");
        map.put("返回首页", "Back to Home");

        // Status text & prompts
        map.put("请稍候", "Please wait");
        map.put("性能优化已启用", "Performance optimization enabled");
        map.put("最大点数", "Max Points");
        map.put("简化容差", "Simplification Tolerance");
        map.put("解析完成，共生成", "Parsing complete, generated");
        map.put("个对象", "objects");
        map.put("解析失败", "Parsing failed");
        map.put("开始解析SVG，预计包含", "Parsing SVG, expected paths");
        map.put("个路径", " paths");
        map.put("正在解析SVG，预计包含", "Parsing SVG, expected");
        map.put("失败", "Failed");
        map.put("成功", "Success");
        map.put("保存失败", "Save failed");
        map.put("保存成功", "Saved successfully");
        map.put("合并G代码失败", "Failed to merge G-code");
        map.put("G代码生成失败", "G-code generation failed");
        map.put("PLT解析失败", "PLT parsing failed");
        map.put("SVG解析失败", "SVG parsing failed");
        map.put("无法读取PLT文件内容。", "Unable to read PLT file.");
        map.put("不支持的文件类型", "Unsupported file type");
        map.put("无法导入矢量数据", "Unable to import vector data");
        map.put("矢量数据导入失败", "Vector import failed");
        map.put("svg", "svg");

        // Misc UI text
        map.put("工具栏", "Toolbar");
        map.put("属性编辑", "Property Editor");
        map.put("返回", "Back");
        map.put("导出", "Export");
        map.put("导出预览", "Export Preview");
        map.put("图层管理", "Layer Manager");
        map.put("导入", "Import");
        map.put("图片", "Image");
        map.put("精选图库", "Featured Gallery");
        map.put("查看更多 >", "View more >");
        map.put("关闭", "Close");
        // toolbar property
        map.put("位置", "Location");
        map.put("X坐标", "X Coordinate");
        map.put("Y坐标", "Y Coordinate");
        map.put("上", "Up");
        map.put("下", "Down");
        map.put("左", "Left");
        map.put("右", "Right");
        map.put("居中", "Center");

        // Additional replacements for punctuation variations
        map.put("：", ":");
        map.put("，", ", ");
        map.put("。", ".");

        return Collections.unmodifiableMap(map);
    }
}