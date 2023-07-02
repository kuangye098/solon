package org.noear.solon.proxy.asm;

public class MethodBean {

    public int access;
    public String methodName;
    public String methodDesc;

    public MethodBean() {
    }

    public MethodBean(int access, String methodName, String methodDesc) {
        this.access = access;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (!(obj instanceof MethodBean)){
            return false;
        }
        MethodBean bean = (MethodBean) obj;

        //access == bean.access //不管访问性，因为代理需要的只是 public
        //                &&

        if (methodName != null
                && bean.methodName != null
                && methodName.equals(bean.methodName)
                && methodDesc != null
                && bean.methodDesc != null
                && methodDesc.equals(bean.methodDesc)){
            return true;
        }
        return false;
    }
}