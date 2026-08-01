package DBMS;

import java.io.Serializable;
import java.util.*;

public class BitmapIndex implements Serializable {
    Hashtable<String,String> dic = new Hashtable<>();
    int currentLength = 0;
    public BitmapIndex(ArrayList<String[]> res, int index) {
        currentLength= res.size();
        for (String[] s: res){
            if (!dic.containsKey(s[index])) {
                dic.put(s[index],"");
            }
        }
        for (String[] s:res){
            List<String> keys = new ArrayList<>(dic.keySet());
            for(String key:keys){
                if (s[index].equals(key))
                    dic.put(s[index],dic.get(key)+"1");
                else {
                    dic.put(key,dic.get(key)+"0");
                }
            }
        }
    }
    public String toString(){
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String,String> entry : dic.entrySet()){
            str.append(entry.getKey()).append("-bitmap index: ").append(entry.getValue()).append("\n");
        }
        return str.toString();
    }
    public String getBitByValue(String value){
        if(!dic.containsKey(value)){
            System.out.println("non indexed key");
            return new String(new char[currentLength]).replace('\0','0');
        }
        return dic.get(value);
    }
    public static ArrayList<String[]> selectIndex(ArrayList<String> bitmapIndices, ArrayList<String[]> res){
        if(bitmapIndices.isEmpty()) return res;
        boolean flag = false;
        ArrayList<String[]> select = new ArrayList<>();
        for(int i=0;i<bitmapIndices.get(0).length();i++){
            flag = false;
            for (String bitmapIndex : bitmapIndices) {
                if (bitmapIndex.charAt(i) != '1') {
                    flag = true;
                    break;
                }
            }
            if (!flag){
                select.add(res.get(i));
            }
        }
        return select;
    }
    public void newRecord(String value){
        List<String> keys = new ArrayList<>(dic.keySet());
        if(!dic.containsKey(value)){
            for (String key:keys){
                dic.put(key,dic.get(key)+'0');
            }
            dic.put(value,new String(new char[currentLength]).replace('\0', '0')+'1');
        } else{
            for (String key:keys){
                dic.put(key,dic.get(key)+(key.equals(value)?'1':'0'));
            }
        }
        System.out.println(this);
        currentLength++;
    }
}
