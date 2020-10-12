package enshud.s2.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Parser().run("data/ts/normal08.ts");
		//System.out.println("tes1");
		new Parser().run("data/ts/normal09.ts");
		//System.out.println("tes2");
		// synerrの確認
		new Parser().run("data/ts/synerr01.ts");
		new Parser().run("data/ts/synerr02.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるParser実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，構文解析を行う．
	 * 構文が正しい場合は標準出力に"OK"を，正しくない場合は"Syntax error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力tsファイル名
	 */
	int gr_index = 0;//インデックス
	public void run(final String inputFileName) {
		String check = null;

		ArrayList<String> get_token_line = new ArrayList<String>();    //トークンの行数
		ArrayList<String> get_id_list = new ArrayList<String>();       //トークのID

		// TODO
		try(BufferedReader in = new BufferedReader(new FileReader(new File(inputFileName)))){  //ファイル読み込み

			String line;
			while((line = in.readLine()) != null) {//分割したトークンをリストへ格納
				String[] get_token = line.split("\t");
				//System.out.println(get_token);
				//IDと行数のみを取得する
				get_id_list.add(get_token[2]);
				get_token_line.add(get_token[3]);
			}
			//for(int i = 0; i<get_id_list.size(); i++) {
			//	System.out.println(get_token_line.get(i));
			//	System.out.println(get_id_list.get(i));
			//}

			//文法判定

			//"program" プログラム名 ";"
			check = is_program(get_id_list,get_token_line,gr_index);
			if(! check.equals("0")) {
				System.err.println("Syntax error: line " + check);
				return;
			}
			//System.out.println("program ok");
			//System.out.println(gr_index);
			//ブロック
			check = is_block(get_id_list,get_token_line,gr_index);
			if(! check.equals("0")) {
				System.err.println("Syntax error: line " + check);
				return;
			}
			//System.out.println("block ok");
			//System.out.println(gr_index);
			//System.out.println(gr_index);
			//複合文
			check = is_compound(get_id_list,get_token_line,gr_index);
			//System.out.println("check" + check);
			if(! check.equals("0")) {
				System.err.println("Syntax error: line " + check);
				return;
			}
			//System.out.println("program ok666");

			//.
			if(! get_id_list.get(gr_index).equals("42")) {
				System.err.println("Syntax error: line " + get_token_line.get(gr_index));
				return;
			}

			System.out.println("OK");



		}
		catch(IOException e) {
			System.err.println("File not found");
		}


	}

	public String is_program(ArrayList<String> id, ArrayList<String> line, int index) {
		//System.out.println("program0 ok");
		//program
		//System.out.println(id.get(index));
		if( ! (id.get(index).equals("17"))) { return line.get(index); }
		index ++;
		//System.out.println("program1 ok");

		//プログラム名
		if( ! (id.get(index).equals("43"))) { return line.get(index); }
		index ++;
		//System.out.println("program2 ok");

		//;
		if( ! id.get(index).equals("37")) { return line.get(index); }
		index ++;
		//System.out.println("program3 ok");

		gr_index = index;
		return "0";


	}

	public String is_block(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		//varでもprocedureでもbeginでもない時エラー
		if( ! id.get(index).equals("21") && ! id.get(index).equals("16") && ! id.get(index).equals("2")) {
			return line.get(index);
		}
		//変数宣言
		//var
		if(id.get(index).equals("21")) {
			check = is_variable_dec(id,line,index);
			if(! check.equals("0")) { return check; }
			index = gr_index;
		}

		//変数宣言がある場合ここまでで判定が終了している
		//ここからは副プログラム宣言郡の判定
		//pocedure
		//副プログラム宣言頭部
		while(true) {
			if(id.get(index).equals("16")) {
				check = is_subprogram_head(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;

				//変数宣言
				// var
				check = is_variable_dec(id,line,index);
				if( ! check.equals("0")) { return check; }
				index = gr_index;

				//複合文
				check = is_compound(id,line,index);
				if( ! check.equals("0")) { return check;}
				index = gr_index;

				//;
				//System.out.println(gr_index);
				if(! id.get(index).equals("37")) {return line.get(index); }
				index ++;
			}

			//System.out.println(gr_index);
			//procedure
			if(id.get(index).equals("16")) {
				continue;
			}
			else {
				break;
			}
		}
		gr_index = index;
		return "0";

	}


	//変数宣言を判定する
	public String is_variable_dec(ArrayList<String> id, ArrayList<String> line ,int index) {
		//変数宣言
		if(id.get(index).equals("21")) {//変数宣言 var
			index ++;
			while(true) {//変数宣言の並び

				//変数名の並び
				//名前
				if( ! id.get(index).equals("43")) { return line.get(index); }
				index ++;

				//: 変数名の並びが終了した時
				if(id.get(index).equals("38")) { index ++; }

				//, 変数名の並びが続く時
				else if(id.get(index).equals("41")) {

					while(true) {
						index ++;

						//,の次に変数名が来なかった時
						//変数名がきた時
						if( ! id.get(index).equals("43")) { return line.get(index); }
						index ++;
						//, もう一度続く
						if(id.get(index).equals("41")) { continue; }
						//: 変数名の並びが終了した場合
						else if(id.get(index).equals("38")) {
							index ++;
							break;
						}
						else { return line.get(index); }

					}

				}else { return line.get(index);}



				//:の後を判定
				//integer char booleanの時
				if(id.get(index).equals("11") || id.get(index).equals("4") || id.get(index).equals("3")) {
					index ++;

					if(id.get(index).equals("37")) {//;
						index ++;
					}
				}
				//arrayの時
				else if(id.get(index).equals("1")) {
					index ++;

					//[
					if( ! id.get(index).equals("35")) { return line.get(index); }
					index ++;

					//符合判定
					if(id.get(index).equals("30") || id.get(index).equals("31")) { index ++; }

					//添字の最小値
					if( ! id.get(index).equals("44")) { return line.get(index); }
					index ++;

					//..
					if( ! id.get(index).equals("39")) { return line.get(index); }
					index ++;

					//符合判定
					if(id.get(index).equals("30") || id.get(index).equals("31")) { index ++; }

					//添字の最大値
					if(! id.get(index).equals("44")) { return line.get(index); }
					index ++;

					//]
					if(! id.get(index).equals("36")) { return line.get(index); }
					index ++;

					//of
					if(! id.get(index).equals("14")) { return line.get(index); }
					index ++;

					//標準型
					if( ! (id.get(index).equals("11") || id.get(index).equals("4") || id.get(index).equals("3"))) {
						return line.get(index);
					}
					index ++;

					//;
					if( ! id.get(index).equals("37")) { return line.get(index); }
					index ++;

				}
				else { return line.get(index); }

				//変数宣言の並びが続くかどうかを判定する
				//名前→変数名の並びのはじめ出ない時はbreak
				if(! id.get(index).equals("43")) { break; }
			}
		}
		gr_index = index;
		return "0";
	}



	//副プログラム宣言頭部
	public String is_subprogram_head(ArrayList<String> id, ArrayList<String> line, int index) {
		//変数宣言がある場合ここまでで判定が終了している
		//ここからは副プログラム宣言郡の判定
		//pocedure
		if( ! id.get(index).equals("16")){ return line.get(index); }
		index ++;

		//名前
		if(! id.get(index).equals("43")) { return line.get(index); }
		index ++;

		//（　仮パラメータの並び
		if(id.get(index).equals("33")) {
			index ++;

			//名前
			if(! id.get(index).equals("43")) { return line.get(index); }
			index ++;

			//仮パラメータが複数あるかどうか判定
			//,
			if(id.get(index).equals("41")) {
				while(true) {
					index ++;

					//,の次に変数名が来なかった時
					if(! id.get(index).equals("43")) { return line.get(index); }
					//変数名がきた時
					index ++;

					//, がきた時→もう一度続く
					if(id.get(index).equals("41")) { continue; }

					//: 変数名の並びが終了した場合
					else if(id.get(index).equals("38")) {
						index ++;
						break;
					}
					else { return line.get(index); }

				}
			}
			//仮パラメータが単数の時
			//：
			else if(id.get(index).equals("38")) { index ++; }
			else { return line.get(index); }

			//標準型
			if( ! (id.get(index).equals("11") || id.get(index).equals("4") || id.get(index).equals("3"))) {
				return line.get(index);
			}
			index ++;

			// ) 仮パラメーター終了
			if(! id.get(index).equals("34")) { return line.get(index); }
			index ++;

			//; 副プログラム頭部終了
			if(! id.get(index).equals("37")) { return line.get(index); }
			index ++;

		}
		//; 副プログラム頭部宣言終了
		else if(id.get(index).equals("37")) {
			index ++;
		}
		else {
			return line.get(index);
		}

		gr_index = index;
		return "0";
	}


	//複合文判定
	public String is_compound(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		//begin
		if(! id.get(index).equals("2")) { return line.get(index); }
		index ++;

		//文の並び
		check = is_sentence(id,line,index);
		if( ! (check.equals("0"))) { return check; }
		index = gr_index;

		//end
		if(! id.get(index).equals("8")) { return line.get(index); }
		index ++;
		//System.out.println("program ok222");

		gr_index = index;
		return "0";

	}

	//文の並び
	public String is_sentence(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		//System.out.println(index + "asd");
		while(true) {
			//文
			//if
			if(id.get(index).equals("10")) {
				index ++;

				//式
				check = is_fomula(id,line,index);
				if(! check.equals("0")) {  return check; }
				index = gr_index;
				//System.out.println("itichika");
				//System.out.println(index);
				//then
				if(! id.get(index).equals("19")) { return line.get(index); }
				index ++;

				//複合文
				check = is_compound(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;

				//elseがあるか判定
				if(id.get(index).equals("7")) {
					index ++;

					//複合文
					check = is_compound(id,line,index);
					if(! check.equals("0")) { return check; }
					index = gr_index;
				}
			}
			//while
			else if(id.get(index) .equals("22")) {
				index ++;

				//式
				check = is_fomula(id,line,index);
				if(! check.equals("0")) {  return check; }
				index = gr_index;
				//System.out.println("itichika");
				//System.out.println(index);
				//do
				if(! id.get(index).equals("6")) { return line.get(index); }
				index ++;
				//System.out.println("itichika");
				//複合文
				check = is_compound(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;
			}
			//基本文
			else{
				check = is_basic_sentence(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;
				//System.out.println(index);
			}

			//System.out.println("itichika");
			//System.out.println(id.get(index));

			//;
			if(! id.get(index).equals("37")) { return line.get(index); }
			index ++;

			//System.out.println("itichika");
			//文が続くかどうかを判定
			//System.out.println(index);
			if(id.get(index).equals("10") || id.get(index).equals("22") || (check = is_basic_sentence(id,line,index)).equals("0")) {
				gr_index = index;
				continue;
			}
			else {
				break;
			}
		}

		gr_index = index;
		return "0";
	}

	//基本文
	public String is_basic_sentence(ArrayList<String> id, ArrayList<String> line, int index) {
		int index_in = index;
		String check1 = null;
		String check2 = null;
		String check3 = null;
		String check4 = null;

		//代入分、手続き呼び出文
		if(id.get(index).equals("43")) {
			index ++;

			//代入分 [ :=
			if(id.get(index).equals("35") || id.get(index).equals("40")) {
				check1 = is_subsituation(id,line,index_in);
				if(! check1.equals("0")) {return check1; }
			}
			//手続き呼び出し文
			else {
				check2 = is_procedure_call(id,line,index_in);
				if(! check2.equals("0")) { return check2; }
			}

		}
		//入出力文
		else if(id.get(index).equals("18") || id.get(index).equals("23")) {
			check3 = is_inout(id,line,index_in);
			if(! check3.equals("0")) { return check3; }
		}
		//複合文
		else if(id.get(index).equals("2")) {
			check4 = is_compound(id,line,index_in);
			if(! check4.equals("0")) { return check4; }
		}
		//どれも当てはまらない
		else {
			return line.get(index);
		}

		return "0";

	}

	//代入文
	public String is_subsituation(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		//変数名
		if(! id.get(index).equals("43")) { return line.get(index); }
		index ++;

		//添字月変数の場合
		//[
		if(id.get(index).equals("35")) {
			index ++;
			//式
			check = is_fomula(id,line,index);
			if(! check.equals("0")) { return check; }
			index = gr_index;
			//]
			if(! id.get(index).equals("36")) { return line.get(index); }
			index ++;
		}

		//:=
		if(! id.get(index).equals("40")) { return line.get(index); }
		index ++;

		//System.out.println(index);
		//式
		check = is_fomula(id,line,index);
		if(! check.equals("0")) { return check;}
		index = gr_index;

		gr_index = index;
		return "0";

	}

	//手続き呼び出し文
	public String is_procedure_call(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		//手続き名
		if(! id.get(index).equals("43")) { return line.get(index); }
		index ++;

		//( があれば
		if(id.get(index).equals("33")) {
			index ++;

			//式の並び
			while(true) {
				//式
				check = is_fomula(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;
				//式の並びが続く時
				//,
				if(id.get(index).equals("41")) {
					index ++;
					continue;
				}
				else {
					break;
				}
			}

			//)
			if(! id.get(index).equals("34")){ return line.get(index); }
			index ++;
		}
		gr_index = index;
		return "0";
	}

	//入出力分
	public String is_inout(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		//readln
		if(id.get(index).equals("18")) {
			index ++;

			//(があれば
			if(id.get(index).equals("33")) {
				index ++;

				//変数の並び
				while(true) {
					//変数名
					if(! id.get(index).equals("43")) { return line.get(index); }
					index ++;

					//添字月変数の場合
					//[
					if(id.get(index).equals("35")) {
						index ++;
						//式
						check = is_fomula(id,line,index);
						if(! check.equals("0")) { return check; }

						index = gr_index;
						//]
						if(! id.get(index).equals("36")) { return line.get(index); }
						index ++;
					}

					//続く時
					//,
					if(id.get(index).equals("41")) {
						index ++;
						continue;
					}
					else {
						break;
					}
				}

				//)
				if(! id.get(index).equals("34")){ return line.get(index); }
				index ++;
			}
		}
		//writeln
		else if(id.get(index).equals("23")) {
			index ++;

			//(があれば
			if(id.get(index).equals("33")) {
				index ++;

				//式の並び
				while(true) {
					//式
					check = is_fomula(id,line,index);
					if(! check.equals("0")) { return check; }

					index = gr_index;
					//式の並びが続く時
					//,
					if(id.get(index).equals("41")) {
						index ++;
						continue;
					}
					else {
						break;
					}
				}

				//)
				if(! id.get(index).equals("34")){ return line.get(index); }
				index ++;
			}
		}
		else {
			return line.get(index);
		}

		gr_index = index;
		return "0";
	}


	//式
	public String is_fomula(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		while(true) {
			//+ or -
			if(id.get(index).equals("30") || id.get(index).equals("31")) { index ++; }

			while(true) {
				//項
				while(true) {
					check = is_factor(id,line,index);
					if(! check.equals("0")) { return check; }
					index = gr_index;

					//乗法演算子があるか
					//*,div,/,mod,and
					if(id.get(index).equals("32") || id.get(index).equals("5") || id.get(index).equals("12") || id.get(index).equals("0")) {
						index ++;
						continue;
					}
					else {
						break;
					}
				}

				//加法演算子
				//+,-,or
				if(id.get(index).equals("30") || id.get(index).equals("31") || id.get(index).equals("15")) {
					index ++;
					continue;
				}
				else {
					break;
				}
			}

			//関係演算子

			if(id.get(index).equals("24") || id.get(index).equals("25") || id.get(index).equals("26")
					|| id.get(index).equals("27") || id.get(index).equals("28")|| id.get(index).equals("29")) {
				index ++;
				continue;
			}
			else {
				break;
			}
		}

		gr_index = index;
		return "0";

	}

	public String is_factor(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		//変数
		if(id.get(index).equals("43")) {
			index ++;

			//添字月変数の場合
			//[
			if(id.get(index).equals("35")) {
				index ++;
				//式
				check = is_fomula(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;
				//]
				if(! id.get(index).equals("36")) { return line.get(index); }
				index ++;
			}
		}
		//定数
		//符合なし整数、文字列、false、true
		else if(id.get(index).equals("44") || id.get(index).equals("45")|| id.get(index).equals("9") || id.get(index).equals("20")) {
			index ++;
			//System.out.println("akakak");
		}
		//式
		// (
		else if(id.get(index).equals("33")) {
			index ++;

			//式
			check = is_fomula(id,line,index);
			if(! check.equals("0")) { return check;}
			index = gr_index;

			//)
			if(! id.get(index).equals("34")) { return line.get(index); }
			index ++;

		}
		//not 因子
		else if(id.get(index).equals("13")) {
			index ++;

			//因子
			check = is_factor(id,line,index);
			if(! check.equals("0")) { return check; }
			index = gr_index;
		}
		else {
			return line.get(index);
		}

		gr_index = index;
		return "0";
	}

}
























