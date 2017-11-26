package ru.android.fractal;

import java.util.ArrayList;

public class Queue {
	private static Queue instance = null;
	private ArrayList<RenderArea> list;
	
	private Queue(){
		list = new ArrayList<RenderArea>();
	}
	
	public static Queue getInstance(){
		if(instance == null){
			instance = new Queue();
		}
		return instance;
	}
	
	public synchronized RenderArea getNext(){
		RenderArea item = null;
		if(list.size() > 0){
			item = list.get(0);
			list.remove(0);
		}
		
		return item;
	}
	
	public synchronized void add(RenderArea area){
		list.add(area);
		//Log.d("Queue.add", area.toString());
	}
	
	public void clear(){
		list.clear();
	}
	
	public boolean isEmpty(){
		return list.size() == 0;
	}
}
