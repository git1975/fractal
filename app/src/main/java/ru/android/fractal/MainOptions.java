package ru.android.fractal;

public class MainOptions {
	public boolean showWhile = true;
	public boolean showPartial = false;
	public int threadCount = 4;
	public int quadCount = 3;
	public int calcDepth = 50;
	public int startType = 2;
	public int precision = 1;
	
	@Override
	public String toString(){
		String result = "showWhile=" + showWhile;
		result = result + ";threadCount=" + threadCount;
		result = result + ";quadCount=" + quadCount;
		result = result + ";calcDepth=" + calcDepth;
		
		return result;
	}
}
