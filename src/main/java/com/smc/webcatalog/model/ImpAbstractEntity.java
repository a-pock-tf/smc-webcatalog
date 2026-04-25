package com.smc.webcatalog.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class ImpAbstractEntity implements Serializable {

	private static final long serialVersionUID = 6264545050112345332L;

	private long id;

	private String sid;

	public Timestamp ctime = new Timestamp(System.currentTimeMillis());
	public Timestamp mtime = new Timestamp(System.currentTimeMillis());
	public String by;
	public boolean active = true;

	public Integer order=0;

	public String pdf1;
	public String pdf2;
	public String pdf3;

	public Integer pdf1_page=1;
	public Integer pdf2_page=1;
	public Integer pdf3_page=1;


	public String cad1;
	public String cad2;
	public String cad3;

	public String img1;
	public String img2;
	public String img3;

	public String link1;
	public String link2;
	public String link3;
	public String link4;
	public String link5;
	public String link6;
	public String link7;
	public String link8;
	public String link9;
	public String link10;

	public String link11;
	public String link12;
	public String link13;
	public String link14;
	public String link15;

    public String link16;
    public String link17;
	public String link18;
	public String link19;
	public String link20;

	public String f1;
	public String f2;
	public String f3;
	public String f4;
	public String f5;

	public String lang;


	public String webcatalog;//総カタPDFリンク
	public String keyword;


	public long getId() {
		return id;
	}



	public void setId(long id) {
		this.id = id;
	}



	public String getSid() {
		return sid;
	}



	public void setSid(String sid) {
		this.sid = sid;
	}



	public Timestamp getCtime() {
		return ctime;
	}



	public void setCtime(Timestamp ctime) {
		this.ctime = ctime;
	}



	public Timestamp getMtime() {
		return mtime;
	}



	public void setMtime(Timestamp mtime) {
		this.mtime = mtime;
	}



	public String getBy() {
		return by;
	}



	public void setBy(String by) {
		this.by = by;
	}



	public boolean getActive() {
		return active;
	}



	public void setActive(boolean active) {
		this.active = active;
	}



	public Integer getOrder() {
		return order;
	}



	public void setOrder(Integer order) {
		this.order = order;
	}



	public String getPdf1() {
		return pdf1;
	}



	public void setPdf1(String pdf1) {
		this.pdf1 = pdf1;
	}



	public String getPdf2() {
		return pdf2;
	}



	public void setPdf2(String pdf2) {
		this.pdf2 = pdf2;
	}



	public String getPdf3() {
		return pdf3;
	}



	public void setPdf3(String pdf3) {
		this.pdf3 = pdf3;
	}



	public Integer getPdf1_page() {
		return pdf1_page;
	}



	public void setPdf1_page(Integer pdf1_page) {
		this.pdf1_page = pdf1_page;
	}



	public Integer getPdf2_page() {
		return pdf2_page;
	}



	public void setPdf2_page(Integer pdf2_page) {
		this.pdf2_page = pdf2_page;
	}



	public Integer getPdf3_page() {
		return pdf3_page;
	}



	public void setPdf3_page(Integer pdf3_page) {
		this.pdf3_page = pdf3_page;
	}



	public String getCad1() {
		return cad1;
	}



	public void setCad1(String cad1) {
		this.cad1 = cad1;
	}



	public String getCad2() {
		return cad2;
	}



	public void setCad2(String cad2) {
		this.cad2 = cad2;
	}



	public String getCad3() {
		return cad3;
	}



	public void setCad3(String cad3) {
		this.cad3 = cad3;
	}



	public String getImg1() {
		return img1;
	}



	public void setImg1(String img1) {
		this.img1 = img1;
	}



	public String getImg2() {
		return img2;
	}



	public void setImg2(String img2) {
		this.img2 = img2;
	}



	public String getImg3() {
		return img3;
	}



	public void setImg3(String img3) {
		this.img3 = img3;
	}



	public String getLink1() {
		return link1;
	}



	public void setLink1(String link1) {
		this.link1 = link1;
	}



	public String getLink2() {
		return link2;
	}



	public void setLink2(String link2) {
		this.link2 = link2;
	}



	public String getLink3() {
		return link3;
	}



	public void setLink3(String link3) {
		this.link3 = link3;
	}



	public String getLink4() {
		return link4;
	}



	public void setLink4(String link4) {
		this.link4 = link4;
	}



	public String getLink5() {
		return link5;
	}



	public void setLink5(String link5) {
		this.link5 = link5;
	}



	public String getLink6() {
		return link6;
	}



	public void setLink6(String link6) {
		this.link6 = link6;
	}



	public String getLink7() {
		return link7;
	}



	public void setLink7(String link7) {
		this.link7 = link7;
	}



	public String getLink8() {
		return link8;
	}



	public void setLink8(String link8) {
		this.link8 = link8;
	}



	public String getLink9() {
		return link9;
	}



	public void setLink9(String link9) {
		this.link9 = link9;
	}



	public String getLink10() {
		return link10;
	}



	public void setLink10(String link10) {
		this.link10 = link10;
	}



	public String getF1() {
		return f1;
	}



	public void setF1(String f1) {
		this.f1 = f1;
	}



	public String getF2() {
		return f2;
	}



	public void setF2(String f2) {
		this.f2 = f2;
	}



	public String getF3() {
		return f3;
	}



	public void setF3(String f3) {
		this.f3 = f3;
	}



	public String getF4() {
		return f4;
	}



	public void setF4(String f4) {
		this.f4 = f4;
	}



	public String getF5() {
		return f5;
	}



	public void setF5(String f5) {
		this.f5 = f5;
	}



	public String getWebcatalog() {
		return webcatalog;
	}



	public void setWebcatalog(String webcatalog) {
		this.webcatalog = webcatalog;
	}



	public String getLang() {
		return lang;
	}



	public void setLang(String lang) {
		this.lang = lang;
	}



	public String getKeyword() {
		return keyword;
	}



	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}



	public String getLink11() {
		return link11;
	}



	public void setLink11(String link11) {
		this.link11 = link11;
	}



	public String getLink12() {
		return link12;
	}



	public void setLink12(String link12) {
		this.link12 = link12;
	}



	public String getLink13() {
		return link13;
	}



	public void setLink13(String link13) {
		this.link13 = link13;
	}



	public String getLink14() {
		return link14;
	}



	public void setLink14(String link14) {
		this.link14 = link14;
	}



	public String getLink15() {
		return link15;
	}



	public void setLink15(String link15) {
		this.link15 = link15;
	}



    public String getLink16() {
        return link16;
    }



    public void setLink16(String link16) {
        this.link16 = link16;
    }



    public String getLink17() {
        return link17;
    }



    public void setLink17(String link17) {
        this.link17 = link17;
    }



    public String getLink18() {
        return link18;
    }



    public void setLink18(String link18) {
        this.link18 = link18;
    }



    public String getLink19() {
        return link19;
    }



    public void setLink19(String link19) {
        this.link19 = link19;
    }



    public String getLink20() {
        return link20;
    }



    public void setLink20(String link20) {
        this.link20 = link20;
    }


}
