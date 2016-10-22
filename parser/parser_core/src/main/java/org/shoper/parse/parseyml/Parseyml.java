package org.shoper.parse.parseyml;

import org.shoper.parse.ParserControl;

import java.util.Map;

/**
 * 解析yml类：抽象处理者
 * Created by jungle on 2016-10-12.
 */
public abstract class Parseyml {
    protected Parseyml successor;//定义继承对象
    protected String name;//参数名
    public Parseyml(String name){
        this.name=name;
    }
    public void setSuccessor(Parseyml successor) {
        this.successor = successor;
    }
    public abstract void processRequest(ParserControl request);
}
