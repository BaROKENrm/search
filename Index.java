package com.HYK.util;

import com.HYK.model.DocInfo;
import com.HYK.model.Weight;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建索引
 * 正排索引：从本地数据中读取到java内存中
 * 倒排索引：构建Map<String,List<信息>>
 * Map键：关键词（分词来做）
 * Map值 - 信息:DocInfo对象引用或者是id，权重（标题出现对应关键词的数量 * 10 + 正文对应关键词的数量 * 1）
 */
public class Index {
    //构建正排索引
    private static final List<DocInfo> FORWARDINDEX = new ArrayList<>();
    //构建倒排索引
    private  static final Map<String,List<Weight>> INVERTEDINDEX = new HashMap<>();
    public static void buildForwardIndex(){
        try {
            FileReader fileReader = new FileReader(Parser.RAWDATA);
            BufferedReader br = new BufferedReader(fileReader);
            int id = 0;
            String line;
            while((line = br.readLine()) != null){//一行一行的读取
                if(line.trim().equals("")){//文件的最后一行是空行
                    continue;
                }
                //一行对应一个DocInfo对象
                DocInfo doc = new DocInfo();
                doc.setId(++id);
                String[] parts = line.split("\3");//每一行按照\3间隔符切割
                doc.setTitle(parts[0]);
                doc.setUrl(parts[1]);
                doc.setContent(parts[2]);
                //添加到正排索引
                FORWARDINDEX.add(doc);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //通过关键词在倒排中查找映射文档
    public static List<Weight> get(String keyWord){
        return INVERTEDINDEX.get(keyWord);
    }


    public static void buildInvertedIndex(){
        for (DocInfo doc:FORWARDINDEX) {
            //对文章和正文进行分词，每一个分词生成一个weight对象，需要计算权重
            //先构造一个HashMap，保存分词和Weight对象
            Map<String,Weight> cache = new HashMap<>();

            /**
             * 对文章标题记性分词
             */
            List<Term> titles = ToAnalysis.parse(doc.getTitle()).getTerms();
            //第一次出现的分词关键词，要new Weight对象，之后在出现相同分词关键词时，要获取之前已经拿到的相同关键词weight对象，在重新计算权重
            for (Term title:titles) {
                Weight w = cache.get(title.getName());
                if(w == null){
                    w = new Weight();
                    w.setDoc(doc);
                    w.setKeyword(title.getName());
                    cache.put(title.getName(),w);
                }
                w.setWeight(w.getWeight() + 10);
            }
            /**
             * 对文章正文进行分词
             */
            List<Term> contents = ToAnalysis.parse(doc.getContent()).getTerms();
            for (Term content:contents){
                Weight w = cache.get(content.getName());
                if(w == null){
                    w = new Weight();
                    w.setDoc(doc);
                    w.setKeyword(content.getName());
                    cache.put(content.getName(),w);
                }
                w.setWeight(w.getWeight() + 1);
            }

            /**
             * 将map数据保存到倒排索引中
             */
            for(Map.Entry<String,Weight> e : cache.entrySet()){
                String keyWord = e.getKey();
                Weight w = e.getValue();
                List<Weight> weights = INVERTEDINDEX.get(keyWord);
                if(weights == null){
                    weights = new ArrayList<>();
                    INVERTEDINDEX.put(keyWord,weights);
                }
                weights.add(w);
            }

        }
    }

    public static void main(String[] args) {
        Index.buildForwardIndex();
//        FORWARDINDEX
//                .stream()
//                .forEach(System.out::println);
        Index.buildInvertedIndex();


    }
}
