/*
 * Result.java
 *
 * Created on 2004/01/13, 19:31
 */

package com.smc.util;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author  miyasit
 * @version
 */
public class ImpSearchResult<E> {

	private List<E> result;//���ʂ�obj���X�g

	private String keyword;
	private ImpNavi prev;
	private ImpNavi next;
	private int start;
	private int end;
	private int max;
	private int limit;
	private LinkedList<ImpNavi> pnavi;


	public ImpSearchResult(int max,int limit,int offset,String enc){
		init(max,limit,offset,0,true);
	}

	public ImpSearchResult(int max,int limit,int offset){
		init(max,limit,offset,0,true);
	}

	public ImpSearchResult(int max,int limit,int offset,int show){
		init(max,limit,offset,show,true);
	}

	public ImpSearchResult(int max,int limit,int offset,int show,boolean asc){
		init(max,limit,offset,show,asc);
	}

	//�Ō�̃����N�ŕ\��
	public int getLastoff(){

		int i = 0;
		if(pnavi!=null&&pnavi.size()>0){
			i = pnavi.get(pnavi.size()-1).getOff();
		}

		return i;
	}

	private void hideNavi(int show){

		//Log.log("Pnavi="+this.getPnavisize()+"/show="+show);

		if(show>0&&this.getPnavisize()>show){
			int size = this.getMax();

			int current = 1;
			for(ImpNavi navi:pnavi){
				if(navi.getIsCurrent()){
					current = Integer.parseInt(navi.getPageno());
				}
			}

			int hidestart = (int)(current-Math.floor(show/2));
			int hideend = (int)(current+Math.floor(show/2));

			//Log.log("start="+hidestart+"/end="+hideend+"/size="+size);

			if(hidestart<0) { hideend = show ;}
			if(hideend>size) { hidestart = current- (show - ( size - current ))+1;};

			int c = 0;
			for(ImpNavi navi:pnavi){
				c++;
				if(c>hidestart&&c<hideend){
					navi.setHide(false);
				}else{
					navi.setHide(true);
				}
			}
		}

	}

    /** Creates new Result */
    public void init(int max,int limit,int offset,int show,boolean asc) {

    	this.max = max;
    	this.limit = limit;
		int pages = max/limit;

		//Log.log("pages="+pages);
		//Log.log("max%limit="+(max%limit));

		if(max%limit>0){//�]�蕪
			pages = pages + 1;
		}

		boolean nextIsCurrent = false;
		boolean prevIsCurrent = false;

        int nextOff=limit+offset;
        int prevOff=offset-limit;
        this.start = offset+1;
        this.end = offset+limit;

		//������
		if(offset<0) offset = 0;
        if(offset>max) offset =max;

        //Log.log("NextOff="+nextOff);
        //Log.log("max="+max);

        if(nextOff>=max){
        	nextOff = max;
        	nextIsCurrent = true ;
        }
        if(prevOff<0){
        	prevOff = 0;
        	prevIsCurrent = true ;
        }
        if(start<0) start = 0;
        if(end>max) end = max;


		//����N��
		ImpNavi n = new ImpNavi();
		n.setPageno("NEXT");
		n.setLimit(limit);
		n.setOff(nextOff);
		n.setIsCurrent(nextIsCurrent);
		//Log.log("nextIsCurrent="+nextIsCurrent);

		this.setNext(n);

		//�O��N��
		n = new ImpNavi();
		n.setPageno("PREV");
		n.setLimit(limit);
		n.setOff(prevOff);
		n.setIsCurrent(prevIsCurrent);
		this.setPrev(n);

		//�y�[�W�߂���
		pnavi = new LinkedList<ImpNavi>();
		LinkedList<ImpNavi> pnavi_asc = new LinkedList<ImpNavi>();

		int offplus = 0;
		for(int i=0;i<pages;i++){
			//����N��
			n = new ImpNavi();
			n.setPageno(String.valueOf(i+1));
			n.setLimit(limit);
			n.setOff(offplus);
			if( (i*limit)+1 == start ){
				n.setIsCurrent(true);
			}
			if(pages-1==i){
				n.setIsLast(true);

			}

			pnavi_asc.add(n);
			if(asc){
				pnavi.add(n);
			}else{
				pnavi.addFirst(n);
			}
			offplus = offplus + limit;
		}

		//...�ŉB���ꍇ
		/*
		LinkedList<Navi> newlist = new LinkedList<Navi>();
		if(pnavi.size()>hidepage&&hidepage>0){

			int hstart = getCurrent() - hidepage/2;
			int hend = getCurrent() + hidepage/2 ;
			if(pages-hstart<hidepage){
				hstart=pages-hidepage;
			}
			if(hend<hidepage){
				hend=hidepage;
			}
			int hidx = 0;
			boolean b_hide_start = false;
			boolean b_hide_end = false;

			for(Navi navi:pnavi_asc){
				hidx++;
				if(hidx>1&&hidx<hstart){
					navi = new Navi();
					navi.setHide(true);
					if(!b_hide_start){
						b_hide_start = true;
						if(asc){
							newlist.add(navi);
						}else{
							newlist.addFirst(navi);
						}
					}
				}else if(hidx<pages&&hidx>hend){
					navi = new Navi();
					navi.setHide(true);
					if(!b_hide_end){
						b_hide_end = true;
						if(asc){
							newlist.add(navi);
						}else{
							newlist.addFirst(navi);
						}
					}
				}else{
					if(asc){
						newlist.add(navi);
					}else{
						newlist.addFirst(navi);
					}
				}



			}
			this.setPnavi(newlist);
		}
		*/
		hideNavi(show);


    }

    public int getSize(){
    	int i=0;
    	if(result!=null) i=result.size();
        return i;
    }

    public int getCurrent(){

    	int c = 0;
		for(ImpNavi navi:pnavi){
			if(navi.getIsCurrent()) c = Integer.parseInt(navi.getPageno());
		}
    	return c;
    }

	/**
	 * @return
	 */
	public String getKeyword() {
		return keyword;
	}


	/**
	 * @return
	 */
	public List<E> getResult() {
		return result;
	}

	/**
	 * @param string
	 */
	public void setKeyword(String string) {
		keyword = string;
	}


	/**
	 * @param result
	 */
	public void setResult(List<E> result) {
		this.result = result;



	}

	public int getPnavisize(){
		return pnavi.size();
	}

	/**
	 * @return
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * @return
	 */
	public ImpNavi getNext() {
		return next;
	}


	/**
	 * @return
	 */
	public ImpNavi getPrev() {
		return prev;
	}

	/**
	 * @return
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @param i
	 */
	public void setEnd(int i) {
		end = i;
	}

	/**
	 * @param navi
	 */
	public void setNext(ImpNavi navi) {
		next = navi;
	}



	/**
	 * @param navi
	 */
	public void setPrev(ImpNavi navi) {
		prev = navi;
	}

	/**
	 * @param i
	 */
	public void setStart(int i) {
		start = i;
	}

	/**
	 * @return
	 */
	public int getMax() {
		return max;
	}

	/**
	 * @return
	 */
	public LinkedList getPnavi() {
		return pnavi;
	}

	/**
	 * @param i
	 */
	public void setMax(int i) {
		max = i;
	}

	/**
	 * @param list
	 */
	public void setPnavi(LinkedList list) {
		pnavi = list;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}




}
