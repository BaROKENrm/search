package com.HYK.Servlet;

import com.HYK.model.Result;
import com.HYK.model.Weight;
import com.HYK.util.Index;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet(value = "/search",loadOnStartup = 0)//启动tomcat时初始化，默认-1，不初始化
public class searchServlet extends HttpServlet {
    /**
     * 初始化操作，创建正排倒排索引
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        Index.buildForwardIndex();
        Index.buildInvertedIndex();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");//ajax请求，相应json格式
        Map<String,Object> map = new HashMap<>();
        String query = req.getParameter("query");//搜索框内容
        List<Result> results = new ArrayList<>();
        try{
            /**
             * 根据搜索内容处理搜索业务
             * 1.根据搜索内容进行分词，遍历每个分词
             * 2.每个分词在倒排中查找相应文档（一个分析对应多个文档）
             * 3.一个文档转换为一个Result（不同分析可能存在相同文档，需要合并）
             * 4.合并完成后，对List<Result>排序，按照权重降序
             */
            if(query == null || query.trim().length() == 0){
                map.put("ok",true);
                map.put("msg","搜索内容为空");
            }
            //1.根据搜索内容进行分词，遍历每个分词
            else{
                for (Term t :ToAnalysis.parse(query).getTerms()){
                    String fenci = t.getName();
                    //2.每个分词在倒排中查找相应文档（一个分析对应多个文档）
                    List<Weight> weights = Index.get(fenci);
                    //3.一个文档转换为一个Result（不同分析可能存在相同文档，需要合并）
                    for (Weight w : weights) {
                        //转换weight为result
                        Result r = new Result();
                        r.setId(w.getDoc().getId());
                        r.setTitle(w.getDoc().getTitle());
                        r.setWeight(w.getWeight());
                        r.setUrl(w.getDoc().getUrl());
                        //文档内容超过60个长度，就隐藏为...
                        String content = w.getDoc().getContent();
                        r.setDesc(content.length() <= 60 ? content : content.substring(0,60) + "...");
                        //TODO 合并
                        results.add(r);
                    }
                }
                //4.合并完成后，对List<Result>排序，按照权重降序
                results.sort(new Comparator<Result>() {
                    @Override
                    public int compare(Result o1, Result o2) {
//                        return Integer.compare(o2.getWeight(),o1.getWeight());
                        return o2.getWeight() - o1.getWeight();
                    }
                });
                map.put("ok",true);
                map.put("data",results);
            }


        }
        catch (Exception e){
            e.printStackTrace();
            map.put("ok",false);
        }
        PrintWriter pw = resp.getWriter();
        //设置响应体：输出map对象序列化为json字符串
        pw.println(new ObjectMapper().writeValueAsString(map));
    }
}
