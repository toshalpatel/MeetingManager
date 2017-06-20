package com.ibm.watson.developer_cloud.android.examples;

/**
 * Created by Megha on 5/14/2017.
 */
public class User {
    String email,pwd,name,company,dept,pno;

    public User(String email, String pwd, String name, String company, String dept, String pno) {
        this.email = email;
        this.pwd = pwd;
        this.name = name;
        this.company = company;
        this.dept = dept;
        this.pno = pno;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getPno() {
        return pno;
    }

    public void setPno(String pno) {
        this.pno = pno;
    }
}
