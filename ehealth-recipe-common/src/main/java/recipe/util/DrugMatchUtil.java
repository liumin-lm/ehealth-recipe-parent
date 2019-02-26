package recipe.util;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.WordDictionary;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * created by shiyuping on 2019/2/12
 */
public class DrugMatchUtil {

    private static JiebaSegmenter segmenter;

    static {
        String pathStr = DrugMatchUtil.class.getClassLoader().getResource("").getPath()+"new-cst.dict";
        File file = new File(pathStr);
        /*System.out.println(file.exists()+"-"+file.getAbsolutePath());*/
        Path path = Paths.get(file.getAbsolutePath());
        WordDictionary.getInstance().loadUserDict(path);
        segmenter = new JiebaSegmenter();
    }

    public static String match(String drugname){
        boolean needReplace = -1 != drugname.indexOf("(") && -1 != drugname.indexOf(")");
        if(needReplace){
            StringBuilder sb = new StringBuilder();
            sb.append(drugname.substring(0,drugname.indexOf("("))).append(drugname.substring(drugname.indexOf(")")+1, drugname.length()));
            drugname = sb.toString();
            /*System.out.println("replace:"+drugname);*/
        }
        List<String> list =  segmenter.sentenceProcess(drugname);
        String key = null;
        int maxLength = 0;
        for(String s : list){
            System.out.println(s);
            int curLength = s.length();
            //关键词超过名字的一半长度，认定为关键词
            if(curLength >= drugname.length()/2){
                key = s;
                break;
            }
            if(curLength > maxLength){
                maxLength = curLength;
                key = s;
            }
        }
        return key;
    }
}
