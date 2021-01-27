package com.HYK.util;

import com.HYK.model.DocInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *步骤一：从本地api目录遍历静态html文件
 * 每一个html需要构建正文索引
 * 正文索引信息List<DocInfo>
 * DocInfo(id,title,content,url)
 */
public class Parser {
    //api目录
    public static final String APIPATH = "E:\\jdk文档\\docs\\api";
    //构建的本地文件正排索引
    public static final String RAWDATA = "F:\\javaWeb\\searchAPI\\raw_data.txt";
    //官方api文档的根路径
    public static final String APIBASEPATH = "https://docs.oracle.com//javase/8/docs/api";

    public static void main(String[] args) throws IOException {
        List<File> htmls = listHtml(new File(APIPATH));
        FileWriter fw = new FileWriter(RAWDATA);
//        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter pw = new PrintWriter(fw,true);//打印输出流，自动刷新
//        for(File html : htmls){
//            System.out.println(html.getAbsolutePath());
//        }
        for (File html : htmls) {
            //一个html解析为DocInfo对象
            DocInfo doc = parseHtml(html);
            //保存正排索引到文件：输出到目标文件,一行一行输出
            String uri = html.getAbsolutePath().substring(APIPATH.length());
            System.out.println("开始" + uri);
            pw.println(doc.getTitle() + "\3" + doc.getUrl() + "\3" + doc.getContent());

        }
    }

    private static DocInfo parseHtml(File html) {
        DocInfo doc = new DocInfo();
        //设置标题为文件html文件名字（去除.html后缀）
        doc.setTitle(html.getName().substring(0,html.getName().length() - ".html".length()));
        //获取相对路径
        String uri = html.getAbsolutePath().substring(APIPATH.length());
        doc.setUrl(APIBASEPATH + uri);
        //设置正文
        doc.setContent(parseContent(html));

        return doc;
    }

    /**
     *解析html文件正文
     * 只取标签内容，有多个标签就拼接
     * <标签> 正文 </标签>
     */
    private static String parseContent(File html) {
        StringBuilder sb = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(html);
            int i;
            boolean isContext = false;
            while((i = fileReader.read()) != -1) {
                char c = (char) i;
                //只是读取正文部分
                if(isContext){//当前是正文部分
                    if(c == '<'){//当读取到结束标签的第一个尖括号，接下来就不是正文部分了，结束读取
                        isContext = false;
                        continue;
                    }
                    else if(c == '\r' || c == '\n'){//当碰到换行符
                        sb.append(" ");
                    }
                    else{
                        sb.append(c);
                    }
                }
                else if(c == '>'){//当前不是正文，当读取到 '>'，接下来就是正文部分，开始读取
                    isContext = true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }


    // 递归方法
    private static List<File> listHtml(File dir){
        List<File> list = new ArrayList<>();
        File[] children = dir.listFiles();//返回某个目录下所有文件和目录的绝对路径
        for (File child:children) {
            if(child.isDirectory()){
                list.addAll(listHtml(child));
            }
            else if(child.getName().endsWith(".html")){//添加html文件
                list.add(child);
            }
        }
        return list;
    }

}
