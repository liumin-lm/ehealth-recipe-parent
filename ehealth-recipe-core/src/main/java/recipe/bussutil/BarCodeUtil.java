package recipe.bussutil;

import org.apache.commons.lang.StringUtils;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.tools.UnitConv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * created by shiyuping on 2020/6/3
 * 条形码生成工具
 */
public class BarCodeUtil {
    private static final Logger logger = LoggerFactory.getLogger(BarCodeUtil.class);

    public static URI generateFileUrl(String msg, String path) {
        if (StringUtils.isEmpty(msg)) {
            return null;
        }
        try {
            File barCodeFile = BarCodeUtil.generateFile(msg, path);
            return barCodeFile.toURI();
        } catch (Exception e) {
            logger.error("BarCodeUtil generateFileUrl wordToPdf ={} ,path={} ,error", msg, path, e);
            return null;
        }
    }

    /**
     * 生成文件
     *
     * @param msg
     * @param path
     * @return
     */
    public static File generateFile(String msg, String path) throws Exception {
        File file = new File(path);
        generate(msg, new FileOutputStream(file));
        return file;
    }

    /**
     * 生成到流
     *
     * @param msg
     * @param ous
     */
    private static void generate(String msg, OutputStream ous) throws Exception {
        if (StringUtils.isEmpty(msg) || ous == null) {
            return;
        }
        Code128Bean bean = new Code128Bean();
        // 精细度
        final int dpi = 150;
        // module宽度// 设置条码每一条的宽度 UnitConv 是barcode4j 提供的单位转换的实体类，用于毫米mm,像素px,英寸in,点pt之间的转换
        final double moduleWidth = UnitConv.in2mm(3.0f / dpi);
        // 配置对象
        bean.setModuleWidth(moduleWidth);
        // 设置两侧是否加空白
        bean.doQuietZone(false);
        // 设置文本位置（包括是否显示）
        bean.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        bean.setFontSize(6);
        // 输出到流
        BitmapCanvasProvider canvas = new BitmapCanvasProvider(ous, "image/png", dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
        // 生成条形码
        bean.generateBarcode(canvas, msg);
        // 结束绘制
        canvas.finish();
        ous.close();
    }
}
