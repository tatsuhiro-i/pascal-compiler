package enshud.s4.compiler;

class Variable_table{
	String name;        //名前
	String name_type;            //種類
	String data_type;        //データ型
	int num_of_var;          //何番目の変数かを格納する
	int array_min,array_max;  //配列の最小値、最大値
	Variable_table next;//次のノードへの参照
}

//線形リストの操作
public class LinkedList {
	private Variable_table first;

	//ノードの追加
	public void addNode(String name, String name_type, String data_type, int num_var) {
		Variable_table node = new Variable_table();
		node.name= name;
		node.name_type = name_type;
		node.data_type = data_type;
		node.num_of_var = num_var;
		node.next = first;
		first = node;
	}

	//配列ノードの追加
	public void addArrNode(String name, String name_type, String data_type, int array_min,int array_max, int num_var) {
		Variable_table node = new Variable_table();
		node.name= name;
		node.name_type = name_type;
		node.data_type = data_type;
		node.array_min = array_min;
		node.array_max = array_max;
		node.num_of_var = num_var;
		node.next = first;
		first = node;
	}

	//名前が存在するかを検索
	public boolean search(String search_name) {
		Variable_table current = first;
		while(current != null) {
			if(current.name.equals(search_name)){
				return true;
			}
			current = current.next;
		}

		return false;
	}

	//検索した型名の情報をとってくる
	public Variable_table var_search(String search_name) {
		Variable_table current = first;
		while(current != null) {
			if(current.name.equals(search_name)){
				return current;
			}
			current = current.next;
		}
		return current;

	}
}
