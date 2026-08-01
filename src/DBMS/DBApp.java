package DBMS;

import javafx.scene.control.Tab;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class DBApp
{
	static int dataPageSize = 2;


	public static void createTable(String tableName, String[] columnsNames)
	{
		Table t = new Table(tableName, columnsNames);
		FileManager.storeTable(tableName, t);
	}

	public static void insert(String tableName, String[] record)
	{
		Table t = FileManager.loadTable(tableName);
		t.insert(record);
		FileManager.storeTable(tableName, t);
	}

	public static ArrayList<String []> select(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select();
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static ArrayList<String []> select(String tableName, int pageNumber, int recordNumber)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select(pageNumber, recordNumber);
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static ArrayList<String []> select(String tableName, String[] cols, String[] vals)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select(cols, vals);
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static String getFullTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		String res = t.getFullTrace();
		return res;
	}

	public static String getLastTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		String res = t.getLastTrace();
		return res;
	}
	//helper
	public static int getIndexOfCol(String tableName, String colName){
		Table t = FileManager.loadTable(tableName);
		if (t==null) return -1;
		String[] strs = t.getColumnsNames();
		for (int i =0;i<strs.length;i++)
			if (strs[i].equals(colName))
				return i;
		return -1;
	}
	public static ArrayList<Integer> getIndices(String[] cols,ArrayList<Integer> indices,String[] tableCol){
		ArrayList<Integer> res = new ArrayList<>();
		for(int index: indices){
			for(int i=0;i<tableCol.length;i++){
				if(cols[index].equals(tableCol[i])){
					System.out.println(tableCol[i]+" is not indexed: linear search");
					res.add(i);
				}
			}
		}
		System.out.println(res);
		return res;
	}
	//ms2
	public static ArrayList<String []> validateRecords(String tableName){
		Table table = FileManager.loadTable(tableName);
		ArrayList<String []> res = new ArrayList<>();
		ArrayList<Integer> indices = new ArrayList<>();
		for(int i=0;i<table.getPageCount();i++){
			Page page = FileManager.loadTablePage(tableName,i);
			if(page==null){
				indices.add(i);
				System.out.println("null page detected");
			}
		}
		if(!indices.isEmpty())
			for(String str:table.getTrace()) {
				if(table.getTrace().indexOf(str)==1 || table.getTrace().indexOf(str)==0) continue;
				String[] d = str.split(",");
				int index = d.length-2;
				if(index<=0) break;
				d = d[index].split(":");
				int pageNumber =  Integer.parseInt(d[1]);
				if(indices.contains(pageNumber)){
					System.out.println(str + "is missing");
					ArrayList<String> tmp = new ArrayList<>();
					int i=10;
					while (i<str.length()&&str.charAt(i)!=']') {
						i++;
					}
					String tmp2 =  str.substring(10,i);
					System.out.println("tmp2 :"+tmp2);
					String[] s = tmp2.split(", ");
					for(int j=0;j<s.length;j++){
						System.out.println("hello"+s[j]);
					}
					res.add(s);
				}
		}
		table.addTrace("Validating records: "+res.size()+" records missing.");
		FileManager.storeTable(tableName, table);
		return res;
	}
	public static void recoverRecords(String tableName, ArrayList<String[]> missing){
		Table table = FileManager.loadTable(tableName);
		Set<Integer> indices = new HashSet<>();
		ArrayList<String> trace = table.getTrace();
		for(String[] strs: missing){
			String str = Arrays.asList(strs).toString();
			for(String s: trace){
				if(!s.startsWith("Inserted:")) continue;
				String[] d = s.split(",");

				int i=10;
				while (i<s.length()&&s.charAt(i)!=']') {
					i++;
				}
				String tmp =  s.substring(9,i+1);
				if(tmp.equals(str)){
					int index = d.length-2;
					int page = Integer.parseInt(d[index].split(":")[1]);
					Page page1 = FileManager.loadTablePage(tableName,page);
					indices.add(page);
					if(page1==null){
						Page page2 = new Page();
						page2.insert(strs);
						FileManager.storeTablePage(tableName,page,page2);
					}
					else{
						page1.insert(strs);
						FileManager.storeTablePage(tableName,page,page1);
					}
					break;
				}

			}
		}
		List<Integer> indexes = new ArrayList<>(indices);
		Collections.sort(indexes);
		table.addTrace("Recovering "+missing.size()+" records in pages: "+indexes);
		FileManager.storeTable(tableName, table);
	}
	public static void createBitMapIndex(String tableName, String colName){
		ArrayList<String[]> res = select(tableName);
		Table table = FileManager.loadTable(tableName);
		table.addIndexed(colName);
		int index = getIndexOfCol(tableName, colName);
		if (index == -1) return;
		BitmapIndex bitmapIndex = new BitmapIndex(res,index);
		System.out.println(bitmapIndex);
		FileManager.storeTableIndex(tableName,colName,bitmapIndex);
		FileManager.storeTable(tableName,table);
	}
	public static String getValueBits(String tableName, String colName, String value){
		BitmapIndex bitmapIndex = FileManager.loadTableIndex(tableName,colName);
		if(bitmapIndex==null) return null;
		return bitmapIndex.getBitByValue(value);
	}
	public static ArrayList<String []> selectIndex(String tableName, String[] cols, String[] vals){
		long startTime = System.currentTimeMillis();
		Table table = FileManager.loadTable(tableName);
		ArrayList<String> bitmapIndices = new ArrayList<>();
		ArrayList<Integer> nonIndexed = new ArrayList<>();
		ArrayList<String[]> res = select(tableName);
		String[] colNames = table.getColumnsNames();
//		Select index condition:[major, gpa]->[CS, 1.2], Indexed columns: [major, gpa], Indexed selection count: 1, Final count: 1, execution time (mil):3

		List<String> tmpColNames = Arrays.asList(cols);
		List<String> tmpVals = Arrays.asList(vals);
		for(int i=0;i<cols.length;i++){
			for(int j=i+1;j<cols.length;j++){
				if(i==j) continue;
				if(tmpColNames.get(i).compareTo(tmpColNames.get(j))>0){
					String tmp1 = tmpColNames.get(i);
					String tmp2 = tmpVals.get(i);
					tmpColNames.set(i, tmpColNames.get(j));
					tmpVals.set(i, tmpVals.get(j));
					tmpColNames.set(j, tmp1);
					tmpVals.set(j, tmp2);
				}
			}
		}
		String trace ="Select index condition:"+ tmpColNames+"->"+tmpVals+", ";
		ArrayList<String> indexedColNames = new ArrayList<>();
		ArrayList<String> nonIndexedColNames = new ArrayList<>();
		int len = -1;
		for(int i=0;i<cols.length&&i<vals.length;i++){
			String str = getValueBits(tableName, cols[i], vals[i]);
			if(str==null) {
				nonIndexed.add(i);
				nonIndexedColNames.add(cols[i]);
				continue;
			}
			indexedColNames.add(cols[i]);
			len = len==-1?str.length():len;
			if(str.length()!=len) {
				System.err.println("indices don't have equal length");
				return null;
			}
            bitmapIndices.add(str);
        }
		ArrayList<String []> bitmapFiltered = BitmapIndex.selectIndex(bitmapIndices,res);
		String[] tmp;
		if(!indexedColNames.isEmpty()){
			tmp=indexedColNames.toArray(new String[indexedColNames.size()]);
			Arrays.sort(tmp);
			trace+="Indexed columns: "+Arrays.asList(tmp)+", Indexed selection count: "+bitmapFiltered.size()+", ";
		}
		if(!nonIndexedColNames.isEmpty()){
			tmp=nonIndexedColNames.toArray(new String[nonIndexedColNames.size()]);
			Arrays.sort(tmp);
			trace+="Non Indexed: "+Arrays.asList(tmp)+", ";
		}
		ArrayList<Integer> indices = getIndices(cols,nonIndexed,colNames);
		ArrayList<String[]> linearFilter = linearFilter(bitmapFiltered,nonIndexed,vals,indices);
		trace+="Final count: "+linearFilter.size()+", execution time (mil): "+(System.currentTimeMillis()-startTime);
		table.addTrace(trace);
		FileManager.storeTable(tableName,table);
		return linearFilter;
	}

	private static ArrayList<String[]> linearFilter(ArrayList<String[]> bitmapFiltered, ArrayList<Integer> nonIndexed, String[] vals, ArrayList<Integer> indices) {
		ArrayList<String[]> res = new ArrayList<>();
		boolean exclude = false;
		if(indices.size()!=nonIndexed.size()) {
			System.out.println("error at linear filter");
			return null;
		}
		for (String[] row : bitmapFiltered) {
			exclude=false;
			for (int i=0;i<indices.size();i++) {
				if(!row[indices.get(i)].equals(vals[nonIndexed.get(i)])) {
					exclude=true;
					break;
				}
			}
			if(!exclude){
				res.add(row);
			}
		}
		return res;
	}

	public static void main(String []args) throws IOException
	{
//		FileManager.reset();
//		String[] cols0 = {"a","b","c","d","e","f","g"};
//		DBApp.createTable("u2d", cols0);
//		DBApp.createBitMapIndex("u2d","b");
//		DBApp.createBitMapIndex("u2d","c");
//		DBApp.createBitMapIndex("u2d","d");
//		DBApp.createBitMapIndex("u2d","f");
//		DBApp.createBitMapIndex("u2d","g");
//		String [][] records_u2d = new String[20][cols0.length];
//		for(int i=0;i<20;i++)
//		{
//			records_u2d[i][0] = cols0[0]+i;
//			for(int j=1;j<cols0.length;j++)
//			{
//				records_u2d[i][j] = cols0[j]+((i%(j+1)));
//			}
//			DBApp.insert("u2d", records_u2d[i]);
//		}
//		select("u2d").forEach(record->{
//			for(String col: record)
//				System.out.print(col+", ");
//			System.out.println();
//		});
//		DBApp.getValueBits("u2d", "b", "b0");
//		String[] cols = {"id","name","major","semester","gpa"};
//		createTable("student", cols);
//		String[] r1 = {"1", "stud1", "CS", "5", "0.9"};
//		insert("student", r1);
//		String[] r2 = {"2", "stud2", "BI", "7", "1.2"};
//		insert("student", r2);
//		String[] r3 = {"3", "stud3", "CS", "2", "0.9"};
//		insert("student", r3);
//		String[] r4 = {"4", "stud4", "DMET", "9", "1.2"};
//		insert("student", r4);
//		String[] r5 = {"5", "stud5", "BI", "4", "3.5"};
//		insert("student", r5);
//		 System.out.println("Output of selecting the whole table content:");
//		ArrayList<String[]> result1 = select("student");
//		 for (String[] array : result1) {
//		 for (String str : array) {
//		 System.out.print(str + " ");
//		 }
//		 System.out.println();
//		 }
//		createBitMapIndex("student","major");
////		createBitMapIndex("student","gpa");
//		System.out.println("bit map for CS : "+getValueBits("student","major","CS"));
//		selectIndex("student",new String[]{"gpa","major"},new String[]{"0.9","CS"}).forEach(
//				a -> {
//					for (String s: a){
//						System.out.print(s+ " ");
//					}
//					System.out.println();
//			}
//		);
//		Table student = FileManager.loadTable("student");
//		System.out.println(student.getLastTrace());
//		 System.out.println("--------------------------------");
//		 System.out.println("Output of selecting the output by position:");
//		ArrayList<String[]> result2 = select("student", 1, 1);
//		 for (String[] array : result2) {
//		 for (String str : array) {
//		 System.out.print(str + " ");
//		 }
//		 System.out.println();
//		 }
//
//		 System.out.println("--------------------------------");
//		 System.out.println("Output of selecting the output by column condition:");
//		ArrayList<String[]> result3 = select("student", new String[]{"gpa"}, new
//		String[]{"1.2"});
//		 for (String[] array : result3) {
//		 for (String str : array) {
//		 System.out.print(str + " ");
//		 }
//		 System.out.println();
//		 }
//		 System.out.println("--------------------------------");
//		System.out.println("Full Trace of the table:");
//		System.out.println(getFullTrace("student"));
//		System.out.println("--------------------------------");
//		System.out.println("Last Trace of the table:");
//		System.out.println(getLastTrace("student"));
//		System.out.println("--------------------------------");
//		System.out.println("The trace of the Tables Folder:");
//		System.out.println(FileManager.trace());
//		FileManager.reset();
//		System.out.println("--------------------------------");
//		System.out.println("The trace of the Tables Folder after resetting:");
//		System.out.println(FileManager.trace());
//

		//recovery
//		FileManager.reset(); String[] cols = {"id","name","major","semester","gpa"};
//		createTable("student", cols); String[] r1 = {"1", "stud1", "CS", "5", "0.9"};
//		insert("student", r1); String[] r2 = {"2", "stud2", "BI", "7", "1.2"};
//		insert("student", r2); String[] r3 = {"3", "stud3", "CS", "2", "2.4"};
//		insert("student", r3); String[] r4 = {"4", "stud4", "CS", "9", "1.2"};
//		insert("student", r4); String[] r5 = {"5", "stud5", "BI", "4", "3.5"};
//		insert("student", r5);
//		//////// This is the code used to delete pages from the table
//		System.out.println("File Manager trace before deleting pages: "+FileManager.trace());
//		String path = FileManager.class.getResource("FileManager.class").toString();
//		File directory = new File(path.substring(6,path.length()-17) + File.separator + "Tables//student" + File.separator);
//		File[] contents = directory.listFiles(); int[] pageDel = {0,2}; for(int i=0;i<pageDel.length;i++) { contents[pageDel[i]].delete(); }
//		////////End of deleting pages code
//		System.out.println("File Manager trace after deleting pages: "+FileManager.trace());
//		ArrayList<String[]> tr = validateRecords("student");
//
//		System.out.println("Missing records count: "+tr.size()); recoverRecords("student", tr);
//		System.out.println("--------------------------------");
//		System.out.println("Recovering the missing records.");
//		tr = validateRecords("student");
//		System.out.println("Missing record count: "+tr.size());
//		System.out.println("File Manager trace after recovering missing records: "+FileManager.trace());
//		System.out.println("--------------------------------");
//		System.out.println("Full trace of the table: ");
//		System.out.println(getFullTrace("student"));
//
//		FileManager.reset();
//
//		String[] cols0 = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q"};
//		DBApp.createTable("z21", cols0);
//		String [][] records_z21 = new String[456][cols0.length];
//		for(int i=0;i<456;i++)
//		{
//			records_z21[i][0] = cols0[0]+i;
//			for(int j=1;j<cols0.length;j++)
//			{
//				records_z21[i][j] = cols0[j]+((i%(j+1)));
//			}
//			DBApp.insert("z21", records_z21[i]);
//		}
//		//first 5 records:
//		//record0: [a0, b0, c0, d0, e0, f0, g0, h0, i0, j0, k0, l0, m0, n0, o0, p0, q0]
//		//record1: [a1, b1, c1, d1, e1, f1, g1, h1, i1, j1, k1, l1, m1, n1, o1, p1, q1]
//		//record2: [a2, b0, c2, d2, e2, f2, g2, h2, i2, j2, k2, l2, m2, n2, o2, p2, q2]
//		//record3: [a3, b1, c0, d3, e3, f3, g3, h3, i3, j3, k3, l3, m3, n3, o3, p3, q3]
//		//record4: [a4, b0, c1, d0, e4, f4, g4, h4, i4, j4, k4, l4, m4, n4, o4, p4, q4]
//		//last 5 records:
//		//record451: [a451, b1, c1, d3, e1, f1, g3, h3, i1, j1, k0, l7, m9, n3, o1, p3, q9]
//		//record452: [a452, b0, c2, d0, e2, f2, g4, h4, i2, j2, k1, l8, m10, n4, o2, p4, q10]
//		//record453: [a453, b1, c0, d1, e3, f3, g5, h5, i3, j3, k2, l9, m11, n5, o3, p5, q11]
//		//record454: [a454, b0, c1, d2, e4, f4, g6, h6, i4, j4, k3, l10, m12, n6, o4, p6, q12]
//		//record455: [a455, b1, c2, d3, e0, f5, g0, h7, i5, j5, k4, l11, m0, n7, o5, p7, q13]
//		ArrayList<String[]> selectBeforeRecovery0 = DBApp.select("z21");
//		ArrayList<String[]> missing0 = new ArrayList<String[]>();
//		int pageCount0 = (int)Math.ceil(456.0/DBApp.dataPageSize);
//		for(int i = 0; i < pageCount0-1; i++)
//		{
//			if(Math.random()>0.75)
//			{
//				File dir_z21 = new File(FileManager.directory.getAbsolutePath()+ File.separator + "z21"+ File.separator+i+".db");
//				dir_z21.delete();
//				for(int j=i*DBApp.dataPageSize; j < i * DBApp.dataPageSize + DBApp.dataPageSize; j++)
//				{
//					missing0.add(records_z21[j]);
//				}
//			}
//		}
//		DBApp.recoverRecords("z21", missing0);
//		ArrayList<String[]> selectAfterRecovery0 = DBApp.select("z21");
//		System.out.println(selectBeforeRecovery0.size());
//		System.out.println(selectAfterRecovery0.size());


		FileManager.reset();

		String[] cols0 = {"a","b","c","d","e","f","g","h","i","j"};
		DBApp.createTable("az6r8", cols0);
		String [][] records_az6r8 = new String[429][cols0.length];
		for(int i=0;i<429;i++)
		{
			records_az6r8[i][0] = cols0[0]+i;
			for(int j=1;j<cols0.length;j++)
			{
				records_az6r8[i][j] = cols0[j]+((i%(j+1)));
			}
			DBApp.insert("az6r8", records_az6r8[i]);
		}
		//first 5 records:
		//record0: [a0, b0, c0, d0, e0, f0, g0, h0, i0, j0]
		//record1: [a1, b1, c1, d1, e1, f1, g1, h1, i1, j1]
		//record2: [a2, b0, c2, d2, e2, f2, g2, h2, i2, j2]
		//record3: [a3, b1, c0, d3, e3, f3, g3, h3, i3, j3]
		//record4: [a4, b0, c1, d0, e4, f4, g4, h4, i4, j4]
		//last 5 records:
		//record424: [a424, b0, c1, d0, e4, f4, g4, h0, i1, j4]
		//record425: [a425, b1, c2, d1, e0, f5, g5, h1, i2, j5]
		//record426: [a426, b0, c0, d2, e1, f0, g6, h2, i3, j6]
		//record427: [a427, b1, c1, d3, e2, f1, g0, h3, i4, j7]
		//record428: [a428, b0, c2, d0, e3, f2, g1, h4, i5, j8]
		String[] ConditionColumns0 = {"g","i","b","d","e"};
		String[] ConditionColumnsValues0 = {"g2","i7","b0","d2","e4"};
		DBApp.createBitMapIndex("az6r8","i");
		DBApp.createBitMapIndex("az6r8","b");
		DBApp.createBitMapIndex("az6r8","d");
		DBApp.selectIndex("az6r8", ConditionColumns0, ConditionColumnsValues0);
		System.out.println(FileManager.loadTable("az6r8").getLastTrace());
	}

}
