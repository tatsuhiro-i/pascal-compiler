package enshud.s4.compiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import enshud.casl.CaslSimulator;

public class Compiler {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// Compilerを実行してcasを生成する
		new Compiler().run("data/ts/normal19.ts", "tmp/out.cas");



		// 上記casを，CASLアセンブラ & COMETシミュレータで実行する
		CaslSimulator.run("tmp/out.cas", "tmp/out.ans");
	}
	/**
	 * TODO
	 *
	 * 開発対象となるCompiler実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，CASL IIプログラムにコンパイルする．
	 * コンパイル結果のCASL IIプログラムは第二引数で指定されたcasファイルに書き出すこと．
	 * 構文的もしくは意味的なエラーを発見した場合は標準エラーにエラーメッセージを出力すること．
	 * （エラーメッセージの内容はChecker.run()の出力に準じるものとする．）
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力tsファイル名
	 * @param outputFileName 出力casファイル名
	 */

	int gr_index = 0;//インデックス
	LinkedList[] code_block = new LinkedList[100];//記号表  最大100個程度のブロック数のコードであると想定する
	int[] code_block_state = new int[100]; //記号表の状態を管理する。-1:まだ記入されていない、1:有効範囲、0:有効範囲外
	int code_block_num = 0;//記号表の管理番号
	int sufix_flag = 0;//添字判定の変数
	int variable_place = 0;//変数がどのブロックにいるかを判定する
	int conditional_flag = 0; //条件式の判定
	String data_type_of_fac = null; //因子のデータ型を格納する
	int fomula_flag = 0;
	int relational_ope_flag = 0;//条件式がbooleanかどうかに使用する
	int call_by_factor_flag = 0;
	int call_by_subsituation = 0;
	String subsituation_type = null;
	int str_counter = 0;//出力文字列の個数をカウントする
	int call_by_writeln = 0; //writelnからの呼び出しを管理
	int num_of_var = 0; //変数の数を格納する
	int write_int_flag = 0;//intの出力を管理する変数
	int write_char_flag = 0;
	int while_count = 0;
	int call_by_while = 0;//whileからの呼び出し管理
	int call_by_if = 0;//ifからの呼び出し管理
	int procedure_num = 0;
	int negative_flag = 0;
	int mul_count = 0;
	int div_count = 0;


	StringBuilder main_part = new StringBuilder();
	StringBuilder main_tmp_part;
	StringBuilder declear_part = new StringBuilder();
	StringBuilder procedure_part = new StringBuilder();



	public static ArrayList<String> get_name_list = new ArrayList<String>(); //トークンの名前
	public void run(final String inputFileName, final String outputFileName) {


		// TODO
		String check = null;

		//記号表の状態を初期化
		for(int i = 0; i < 100; i++) {
			code_block_state[i] = -1;
		}

		//初期化
		get_name_list.clear();


		ArrayList<String> get_token_line = new ArrayList<String>();    //トークンの行数
		ArrayList<String> get_id_list = new ArrayList<String>();       //トークのID

		// TODO
		try(BufferedReader in = new BufferedReader(new FileReader(new File(inputFileName)))){  //ファイル読み込み

			//ファイル書き込み
			FileWriter file = new FileWriter(outputFileName);
			PrintWriter pw = new PrintWriter(new BufferedWriter(file));

			String line;
			while((line = in.readLine()) != null) {//分割したトークンをリストへ格納
				String[] get_token = line.split("\t");
				//System.out.println(get_token);
				//IDと行数のみを取得する
				get_name_list.add(get_token[0]);
				get_id_list.add(get_token[2]);
				get_token_line.add(get_token[3]);
			}
			//for(int i = 0; i<get_id_list.size(); i++) {
			//	System.out.println(get_token_line.get(i));
			//	System.out.println(get_id_list.get(i));
			//}

			//casファイル先頭の書き出し
			pw.println("CASL" + "\t" + "START" + "\t" + "BEGIN");
			pw.println("BEGIN" + "\t" + "LAD" + "\t" + "GR6, 0");
			pw.println("\t" + "LAD" + "\t" + "GR7, LIBBUF");


			//文法判定

			//"program" プログラム名 ";"
			check = is_program(get_id_list,get_token_line,gr_index);
			if(! check.equals("0")) {
				System.err.println(check);
				return;
			}
			//System.out.println("program ok");
			//System.out.println(gr_index);
			//ブロック
			check = is_block(get_id_list,get_token_line,gr_index);
			if(! check.equals("0")) {
				System.err.println(check);
				return;
			}
			//System.out.println("block ok");
			//System.out.println(gr_index);
			//System.out.println(gr_index);
			//複合文
			check = is_compound(get_id_list,get_token_line,gr_index);
			//System.out.println("check" + check);
			if(! check.equals("0")) {
				System.err.println(check);
				return;
			}
			//System.out.println("program ok666");

			//.
			if(! get_id_list.get(gr_index).equals("42")) {
				System.err.println("Syntax error: line " + get_token_line.get(gr_index));
				return;
			}

			//VAR
			//LIBBUF
			//ENDの追加
			if(num_of_var > 0) {
				declear_part.append("VAR" + "\t" + "DS" + "\t" + String.valueOf(num_of_var) + "\n");
			}
			declear_part.append("LIBBUF" + "\t" + "DS" + "\t" + "256" + "\n");
			declear_part.append("\t" + "END" + "\n");

			//メインの書き込み
			//RETの追加
			main_part.append("\tRET\n");
			pw.print(main_part);
			pw.print(procedure_part);
			//宣言部分の書き込み
			pw.print(declear_part);

			//サブルーチン書き込み
			try {
				File lib_file = new File("data/cas/lib.cas");
				FileReader lib_filereader = new FileReader(lib_file);
				BufferedReader br = new BufferedReader(lib_filereader);

				String str = br.readLine();

				while(str != null) {
					pw.println(str);
					str = br.readLine();
				}

				br.close();
			}catch(IOException e){
			  System.out.println(e);
			}

			//System.out.println("OK");
			pw.close();




		}
		catch(IOException e) {
			System.err.println("File not found");
		}


	}

	public String is_program(ArrayList<String> id, ArrayList<String> line, int index) {
		//System.out.println("program0 ok");
		//program
		//System.out.println(id.get(index));
		if( ! (id.get(index).equals("17"))) { return "Syntax error: line " + line.get(index); }
		index ++;
		//System.out.println("program1 ok");

		//プログラム名
		if( ! (id.get(index).equals("43"))) { return "Syntax error: line " + line.get(index); }
		index ++;
		//System.out.println("program2 ok");

		//;
		if( ! id.get(index).equals("37")) { return "Syntax error: line " + line.get(index); }
		index ++;
		//System.out.println("program3 ok");

		gr_index = index;
		return "0";


	}

	public String is_block(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		//varでもprocedureでもbeginでもない時エラー
		if( ! id.get(index).equals("21") && ! id.get(index).equals("16") && ! id.get(index).equals("2")) {
			return "Syntax error: line " + line.get(index);
		}

		//記号表生成
		code_block[code_block_num] = new LinkedList();
		code_block_state[code_block_num] = 1;
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
				//新しいコードブロックを生成する
				code_block_num ++;
				code_block[code_block_num] = new LinkedList();
				code_block_state[code_block_num] = 1;
				main_tmp_part = new StringBuilder();

				//1時的に保管する
				main_tmp_part.append(main_part);
				main_part = new StringBuilder();
				//副プログラムの数カウント
				procedure_num ++;
				main_part.append("PROC" + String.valueOf(procedure_num) + "\t" + "NOP"+ "\n");


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
				if(! id.get(index).equals("37")) {return "Syntax error: line " + line.get(index); }
				//記号表の状態を範囲外に設定する
				code_block_state[code_block_num] = 0;
				index ++;

				main_part.append("\tRET\n");

				//副プログラム宣言部分にコピー
				procedure_part.append(main_part);

				//メインパートを復元
				main_part = new StringBuilder();
				main_part.append(main_tmp_part);
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
				if( ! id.get(index).equals("43")) { return "Syntax error: line " + line.get(index); }



				//変数名の取得
				ArrayList<String> variable_name = new ArrayList<String>();
				//変数名が重複しているかを調べる
				if(sematic_check_double_vriable(get_name_list.get(index),code_block[code_block_num])) {
					return "Semantic error: line " + line.get(index);
				}
				variable_name.add(get_name_list.get(index));
				index ++;

				//: 変数名の並びが終了した時
				if(id.get(index).equals("38")) { index ++; }

				//, 変数名の並びが続く時
				else if(id.get(index).equals("41")) {

					while(true) {
						index ++;

						//,の次に変数名が来なかった時
						//変数名がきた時
						if( ! id.get(index).equals("43")) { return "Syntax error: line " + line.get(index); }



						//変数名が重複しているかを調べる
						if(sematic_check_double_vriable(get_name_list.get(index),code_block[code_block_num])) {
							return "Semantic error: line " + line.get(index);
						}
						//変数名の追加
						variable_name.add(get_name_list.get(index));
						index ++;
						//, もう一度続く
						if(id.get(index).equals("41")) { continue; }
						//: 変数名の並びが終了した場合
						else if(id.get(index).equals("38")) {
							index ++;
							break;
						}
						else { return "Syntax error: line " + line.get(index); }

					}

				}else { return "Syntax error: line " + line.get(index);}



				//:の後を判定
				//integer char booleanの時
				if(id.get(index).equals("11") || id.get(index).equals("4") || id.get(index).equals("3")) {

					//記号表へ変数情報を追加する
					for(int i = 0; i< variable_name.size(); i ++) {
						//何番目の変数かを格納する
						num_of_var ++;
						code_block[code_block_num].addNode(variable_name.get(i), "variable",id.get(index),num_of_var);
					}
					//記号表に追加した変数の削除
					variable_name.clear();

					index ++;

					if(id.get(index).equals("37")) {//;
						index ++;
					}
				}
				//arrayの時
				else if(id.get(index).equals("1")) {
					index ++;

					//[
					if( ! id.get(index).equals("35")) { return "Syntax error: line " + line.get(index); }
					index ++;

					//符合判定
					int negative_flag = 0;
					if(id.get(index).equals("30")) {
						index++;
					}
					else if(id.get(index).equals("30")) {
						negative_flag = 1;
						index++;
					}

					//添字の最小値
					if( ! id.get(index).equals("44")) { return "Syntax error: line " + line.get(index); }

					//配列の要素数の記録
					//+の時
					int array_min,array_max;
					if(negative_flag == 0) {
						//System.out.println(get_name_list.get(index));
						array_min = Integer.parseInt(get_name_list.get(index));
					}
					//-の時
					else {
						array_min = -(Integer.parseInt(get_name_list.get(index)));
					}

					//初期化
					negative_flag = 0;
					index ++;

					//..
					if( ! id.get(index).equals("39")) { return "Syntax error: line " + line.get(index); }
					index ++;

					//符合判定
					if(id.get(index).equals("30") || id.get(index).equals("31")) {
						if(id.get(index).equals("30")) {
							index ++;
						}
						else if(id.get(index).equals("30")) {
							negative_flag = 1;
							index++;
						}
					}


					//添字の最大値
					if(! id.get(index).equals("44")) { return "Syntax error: line " + line.get(index); }

					if(negative_flag == 0) {
						array_max = Integer.parseInt(get_name_list.get(index));
					}
					//-の時
					else {
						array_max = -(Integer.parseInt(get_name_list.get(index)));
					}

					//初期化
					negative_flag = 0;
					index ++;

					//]
					if(! id.get(index).equals("36")) { return "Syntax error: line " + line.get(index); }
					index ++;

					//of
					if(! id.get(index).equals("14")) { return "Syntax error: line " + line.get(index); }
					index ++;

					//標準型
					if( ! (id.get(index).equals("11") || id.get(index).equals("4") || id.get(index).equals("3"))) {
						return "Syntax error: line " + line.get(index);
					}

					//記号表へ追加
					for(int i = 0; i < variable_name.size(); i ++) {
						//並列の先頭のアドレスを格納する
						num_of_var ++;
						code_block[code_block_num].addArrNode(variable_name.get(i),"array",id.get(index),array_min,array_max,num_of_var);
						//配列の残りの要素分のアドレスを確保
						num_of_var = num_of_var + array_max - array_min;
					}
					//追加したものを削除
					variable_name.clear();
					index ++;

					//;
					if( ! id.get(index).equals("37")) { return "Syntax error: line " + line.get(index); }
					index ++;

				}
				else { return "Syntax error: line " + line.get(index); }

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
		int num_of_tmpvar = 0;
		//pocedure
		if( ! id.get(index).equals("16")){ return "Syntax error: line " + line.get(index); }
		index ++;

		//名前
		if(! id.get(index).equals("43")) { return "Syntax error: line " + line.get(index); }

		//副プログラム名を記号表に追加
		code_block[code_block_num].addNode(get_name_list.get(index), "sub_program", "char",procedure_num);
		index ++;

		//（　仮パラメータの並び
		if(id.get(index).equals("33")) {
			index ++;

			//変数名を1時的に格納する可変リスト
			ArrayList<String> variable_name = new ArrayList<String>();
			while(true) {
				//名前
				if(! id.get(index).equals("43")) { return "Syntax error: line " + line.get(index); }
				num_of_tmpvar ++;

				//変数名が重複しているかを調べる
				if(sematic_check_double_vriable(get_name_list.get(index),code_block[code_block_num])) {
					return "Semantic error: line " + line.get(index);
				}
				//変数名の取得
				variable_name.add(get_name_list.get(index));
				index ++;

				//仮パラメータが複数あるかどうか判定
				//,
				if(id.get(index).equals("41")) {
					while(true) {
						index ++;

						//,の次に変数名が来なかった時
						if(! id.get(index).equals("43")) { return "Syntax error: line " + line.get(index); }
						//変数名が重複しているかを調べる
						if(sematic_check_double_vriable(get_name_list.get(index),code_block[code_block_num])) {
							return "Semantic error: line " + line.get(index);
						}
						variable_name.add(get_name_list.get(index));
						//変数名がきた時
						num_of_tmpvar ++;
						index ++;

						//, がきた時→もう一度続く
						if(id.get(index).equals("41")) { continue; }

						//: 変数名の並びが終了した場合
						else if(id.get(index).equals("38")) {
							index ++;
							break;
						}
						else { return "Syntax error: line " + line.get(index); }

					}
				}
				//仮パラメータが単数の時
				//：
				else if(id.get(index).equals("38")) { index ++; }
				else { return "Syntax error: line " + line.get(index); }

				//標準型
				if( ! (id.get(index).equals("11") || id.get(index).equals("4") || id.get(index).equals("3"))) {
					return "Syntax error: line " + line.get(index);
				}

				//記号表へ変数情報を追加する
				for(int i = 0; i< variable_name.size(); i ++) {
					num_of_var ++;
					code_block[code_block_num].addNode(variable_name.get(i), "variable",id.get(index),num_of_var);

				}
				//記号表に追加した変数の削除
				variable_name.clear();

				index ++;

				//仮パラメータ名の並びが続く時
				if(id.get(index).equals("37")) {
					index ++;
				}
				//仮パラメータ名の並びが続かない時
				else {
					break;
				}
			}

			// ) 仮パラメーター終了
			if(! id.get(index).equals("34")) { return "Syntax error: line " + line.get(index); }
			index ++;

			//; 副プログラム頭部終了
			if(! id.get(index).equals("37")) { return "Syntax error: line " + line.get(index); }
			index ++;



		}
		//; 副プログラム頭部宣言終了
		else if(id.get(index).equals("37")) {
			index ++;
		}

		else {
			return "Syntax error: line "  + line.get(index);
		}
		if(num_of_tmpvar == 0) {
			//スタックポインタの取り出し
			main_part.append("\t" + "LD" + "\t" + "GR1, GR8" + "\n");
			main_part.append("\t" + "ADDA" + "\t" + "GR1, =1" + "\n");
		}
		else {

			for(int i = 0; i < num_of_tmpvar; i ++) {
				//スタックポインタの取り出し
				main_part.append("\t" + "LD" + "\t" + "GR1, GR8" + "\n");
				main_part.append("\t" + "ADDA" + "\t" + "GR1, =1" + "\n");

				//引数を取得
				main_part.append("\t" + "LD" + "\t" + "GR2, 0, GR1" + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR4, =" + String.valueOf(num_of_var - 1 - i) +"\n");
				main_part.append("\t" + "ST" + "\t" + "GR2, VAR, GR4" + "\n");
				main_part.append("\t" + "SUBA" + "\t" + "GR1, =1" + "\n");

				//スタック内の引数削除
				main_part.append("\t" + "LD" + "\t" + "GR1, 0, GR8" + "\n");
				main_part.append("\t" + "ADDA" + "\t" + "GR8, =1" + "\n");
				main_part.append("\t" + "ST" + "\t" + "GR1, 0, GR8" + "\n");
			}
		}




		gr_index = index;
		return "0";
	}


	//複合文判定
	public String is_compound(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		//begin
		if(! id.get(index).equals("2")) { return "Syntax error: line " + line.get(index); }
		index ++;

		//文の並び
		check = is_sentence(id,line,index);
		if( ! (check.equals("0"))) { return check; }
		index = gr_index;

		//end
		if(! id.get(index).equals("8")) { return "Syntax error: line " + line.get(index); }
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

				call_by_if = 1;
				conditional_flag = 1;
				int while_index = while_count;
				//式
				check = is_fomula(id,line,index);
				if(! check.equals("0")) {  return check; }
				conditional_flag = 0;
				index = gr_index;

				call_by_if = 0;


				main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
				main_part.append("\t" + "CPA" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("\t" + "JZE" + "\t" + "ELSE" +  String.valueOf(while_index) + "\n");


				//then
				if(! id.get(index).equals("19")) { return "Syntax error: line " + line.get(index); }
				index ++;

				//複合文
				while_count ++;
				check = is_compound(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;


				//elseがあるか判定
				if(id.get(index).equals("7")) {
					index ++;


					main_part.append("\t"+ "JUMP" + "\t" + "ENDIF" + String.valueOf(while_index) + "\n");
					//ELSE追加
					main_part.append("ELSE" + String.valueOf(while_index) +"\t" + "NOP" + "\n");
					//複合文
					check = is_compound(id,line,index);
					if(! check.equals("0")) { return check; }
					index = gr_index;
					main_part.append("ENDIF" + String.valueOf(while_index) +"\t" + "NOP" + "\n");
				}
				else {
					//ELSE追加
					main_part.append("ELSE" + String.valueOf(while_index) +"\t" + "NOP" + "\n");
				}
			}
			//while
			else if(id.get(index) .equals("22")) {
				index ++;

				conditional_flag = 1;
				call_by_while = 1;
				int while_index = while_count;

				//while部分の追加
				main_part.append("LOOP" + String.valueOf(while_index) + "\t" + "NOP" + "\n");

				//式
				check = is_fomula(id,line,index);
				if(! check.equals("0")) {  return check; }
				index = gr_index;

				conditional_flag = 0;
				call_by_while = 0;

				//分岐cas


				main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
				main_part.append("\t" + "CPL" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("\t" + "JZE" + "\t" + "ENDLP" +  String.valueOf(while_index) + "\n");


				//do
				if(! id.get(index).equals("6")) { return "Syntax error: line " + line.get(index); }
				index ++;
				//System.out.println("itichika");
				//複合文
				while_count ++;
				check = is_compound(id,line,index);
				if(! check.equals("0")) { return check; }
				index = gr_index;

				//ENDLOOP
				main_part.append("\t" + "JUMP" + "\t" + "LOOP" +  String.valueOf(while_index) + "\n");
				main_part.append("ENDLP" +  String.valueOf(while_index) + "\t" + "NOP" + "\n");


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
			if(! id.get(index).equals("37")) { return "Syntax error: line " + line.get(index); }
			index ++;

			//System.out.println("itichika");
			//文が続くかどうかを判定
			//System.out.println(index);
			//endの場合文が終了
			if(id.get(index).equals("8")) {
				break;
			}
			else {
				continue;
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
			return "Syntax error: line " + line.get(index);
		}

		return "0";

	}

	//代入文
	public String is_subsituation(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		String left_var_name="";
		String left_name_type="";
		String left_data_type="";
		int left_array_min = 0,left_array_max = 0;
		int num_variable = 0;//何番目の変数かを格納する
		int array_flag = 0;//配列かどうかを管理する変数

		//変数名
		if(! id.get(index).equals("43")) { return "Syntax error: line " + line.get(index); }

		//まず変数名があるかどうかを確認する。
		if(is_block_has_var(index)) {
			Variable_table variable = code_block[variable_place].var_search(get_name_list.get(index));
			left_var_name = variable.name;
			left_name_type = variable.name_type;
			left_data_type = variable.data_type;
			num_variable = variable.num_of_var;
			//配列の場合
			if(left_name_type.equals("array")) {
				left_array_min = variable.array_min;
				left_array_max = variable.array_max;
			}
		}
		//存在しない変数が使われている場合
		else {
			return "Semantic error: line " + line.get(index);
		}
		index ++;

		//添字月変数の場合
		//[
		if(id.get(index).equals("35")) {
			index ++;

			//添字から式が呼び出される状態を管理するフラッグ
			sufix_flag = 1;
			array_flag = 1;
			//式
			check = is_fomula(id,line,index);
			if(! check.equals("0")) { return check; }

			//代入分の左辺の配列の添字はGR3で保持する
			main_part.append("\t" + "POP" + "\t" + "GR3" + "\n");
			index = gr_index;
			sufix_flag = 0;
			//]
			if(! id.get(index).equals("36")) { return "Syntax error: line " +line.get(index); }
			index ++;
		}
		//左辺に配列の名前が指定されているとき
		else {
			if(left_name_type.equals("array")) {
				return "Semantic error: line " + line.get(index);
			}
		}



		//:=
		if(! id.get(index).equals("40")) { return "Syntax error: line " +line.get(index); }
		index ++;

		//System.out.println(index);
		//式
		call_by_subsituation = 1;
		subsituation_type = left_data_type;
		check = is_fomula(id,line,index);
		call_by_subsituation = 0;

		if(! check.equals("0")) { return check;}
		index = gr_index;



		//代入分の左辺
		//配列の場合
		if(array_flag == 1) {
			if(num_variable > 1) {
				main_part.append("\t" + "ADDA" + "\t" +" GR3, ="+ String.valueOf(num_variable-2) + "\n");
			}
			else if(num_variable == 1) {
				main_part.append("\t" + "ADDA" + "\t" +" GR3, ="+ String.valueOf(num_variable-1) + "\n");
				main_part.append("\t" + "SUBA" + "\t" +" GR3, ="+ 1 + "\n");
			}
			sub_left(array_flag);
			array_flag = 0;

		}
		//配列じゃない場合
		else {
			main_part.append("\t" + "LD" + "\t" + "GR2, ="+ String.valueOf(num_variable-1)  + "\n");
			sub_left(array_flag);
		}




		gr_index = index;
		return "0";

	}

	//手続き呼び出し文
	public String is_procedure_call(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;

		//手続き名
		if(! id.get(index).equals("43")) { return "Syntax error: line " +line.get(index); }

		//手続き呼び出し名が宣言されているか
		if(! is_block_has_procedure_call(index)) {	return "Semantic error: line " + line.get(index);}
		Variable_table variable = code_block[variable_place].var_search(get_name_list.get(index));
		int variable_num = variable.num_of_var;

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
			if(! id.get(index).equals("34")){ return "Syntax error: line " +line.get(index); }
			index ++;
		}

		main_part.append("\t" + "CALL" + "\t" + "PROC" + String.valueOf(variable_num) + "\n");
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
					if(! id.get(index).equals("43")) { return "Syntax error: line " +line.get(index); }
					index ++;

					//添字月変数の場合
					//[
					if(id.get(index).equals("35")) {
						index ++;


						//式
						check = is_fomula(id,line,index);
						if(! check.equals("0")) { return check; }

						//integerのみ
						//符合かint変数か数字

						//idetifer or 数字
						if(id.get(index).equals("44") || id.get(index).equals("43")){
							index ++;
							//符合　or ]
							//+ or -
							while(true) {
								if(!(id.get(index).equals("30") ||  id.get(index).equals("31")|| id.get(index).equals("32") || id.get(index).equals("5"))) { break;}
								index ++;
								//identifer or 数字
								if(!(id.get(index).equals("44") || id.get(index).equals("43"))){
									return "Semantic error: line " + line.get(index);
								}
								index++;
							}
						}
						//符合　+ or -
						else if(id.get(index).equals("30") || id.get(index).equals("31")) {
							index++;

							//idetifer or 数字
							if(id.get(index).equals("44") || id.get(index).equals("43")){
								index ++;
								//符合　or ]
								//+ or -
								while(true) {
									//+ or -
									if(!(id.get(index).equals("30") ||  id.get(index).equals("31"))) { break;}
									index ++;
									//identifer or 数字
									if(!(id.get(index).equals("44") || id.get(index).equals("43"))){
										return "Semantic error: line " + line.get(index);
									}
									index++;
								}
							}
						}
						//integerじゃない
						else {
							return "Semantic error: line " + line.get(index);
						}


						index = gr_index;
						//]
						if(! id.get(index).equals("36")) { return "Syntax error: line " +line.get(index); }
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
				if(! id.get(index).equals("34")){ return "Syntax error: line " +line.get(index); }
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
					call_by_writeln = 1;
					check = is_fomula(id,line,index);
					call_by_writeln = 0;
					if(! check.equals("0")) { return check; }

					index = gr_index;
					//intの出力
					if(write_int_flag == 1) {
						write_int_cas();
						write_int_flag = 0;
					}
					else if(write_char_flag == 1) {
						write_char_cas();
						write_char_flag = 0;
					}
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
				if(! id.get(index).equals("34")){ return "Syntax error: line " +line.get(index); }
				index ++;
			}
			main_part.append("\t" + "CALL" + "\t" + "WRTLN" + "\n");
		}
		else {
			return "Syntax error: line " +line.get(index);
		}

		gr_index = index;
		return "0";
	}


	//代入分から呼び出された式であることを管理する変数
	int call_by_sub_fomula = 0;
	//式
	public String is_fomula(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		String data_type_of_fomula = null;
		int conditional_check = 0;
		int subsituation_check = 0;
		int add_ope_flag = 0;
		int sub_ope_flag = 0;
		int mul_ope_flag = 0;
		int div_ope_flag = 0;
		int and_ope_flag = 0;
		int or_ope_flag = 0;
		int equals_flag = 0;
		int not_equals_flag = 0;
		int more_than_flag = 0;
		int or_more_flag = 0;
		int less_than_flag = 0;
		int or_less_flag = 0;
		int num_relational = 0;
		int mod_ope_flag = 0;
		int negative_counter = 0;

		if(conditional_flag == 1) {
			conditional_check = 1;
			conditional_flag = 0;
		}

		if(call_by_subsituation == 1) {
			subsituation_check = 1;
			call_by_subsituation = 0;
		}



		int roop_flag = 0;

		while(true) {
			//+ or -
			if(id.get(index).equals("30") || id.get(index).equals("31")) {
				negative_flag = 1;
				index ++;
			}

			while(true) {
				//項
				while(true) {
					//代入分から呼び出された式であることを管理する
					if(subsituation_check ==1) {
						call_by_sub_fomula = 1;
					}
					check = is_factor(id,line,index);

					//意味解析
					if(roop_flag == 0 || fomula_flag == 1) {
						data_type_of_fomula = data_type_of_fac;
						fomula_flag = 0;
						roop_flag = 1;
					}
					//演算子と一演算子
					else {
						if(! data_type_of_fomula.equals(data_type_of_fac)) {
							return "Semantic error: line " + line.get(index);
						}
					}

					if(! check.equals("0")) { return check; }
					index = gr_index;

					if(mul_ope_flag == 1) {
						write_mul_cas();
						if(negative_counter % 2 == 1) {
							main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
							main_part.append("\t" + "LD" + "\t" + "GR1, =0" + "\n");
							main_part.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\n");
							main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
						}
						negative_counter = 0;
						mul_ope_flag = 0;
					}
					else if(div_ope_flag == 1) {
						write_div_cas();
						if(negative_counter % 2 == 1) {
							main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
							main_part.append("\t" + "LD" + "\t" + "GR1, =0" + "\n");
							main_part.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\n");
							main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
						}
						div_ope_flag = 0;
						negative_counter = 0;
					}
					else if(mod_ope_flag == 1) {
						write_mod_cas();
						mod_ope_flag = 0;
						negative_counter = 0;
					}
					else if(and_ope_flag == 1) {
						write_and_cas();
						and_ope_flag = 0;
						negative_counter = 0;
					}
					//乗法演算子があるか
					//*,div,/,mod,and
					if(id.get(index).equals("32") || id.get(index).equals("5") || id.get(index).equals("12") || id.get(index).equals("0")) {
						//andの場合は前後が一時的に異なる型になる可能性がある
						//例）i and 4 < b
						if(id.get(index).equals("0")) {
							roop_flag = 0;
						}

						//掛け算
						if(id.get(index).equals("32")) {
							if(negative_flag == 1) {
								negative_counter ++;
								negative_flag = 0;
							}
							mul_ope_flag = 1;
						}
						//割り算
						else if(id.get(index).equals("5")) {
							if(negative_flag == 1) {
								negative_counter ++;
								negative_flag = 0;
							}
							div_ope_flag = 1;
						}
						//mod
						else if(id.get(index).equals("12")) {
							mod_ope_flag = 1;
						}
						//and
						else if(id.get(index).equals("0")) {
							and_ope_flag = 1;
						}

						index ++;
						continue;
					}
					else {
						break;
					}

				}

				//足算、引き算の実行
				if(add_ope_flag == 1) {
					write_add_cas();
					add_ope_flag = 0;
					negative_counter = 0;
				}
				else if(sub_ope_flag ==1 ) {
					write_sub_cas();
					sub_ope_flag = 0;
					negative_counter = 0;
				}
				else if(or_ope_flag == 1) {
					write_or_cas();
					negative_counter = 0;
				}

				//加法演算子
				//+,-,or
				if(id.get(index).equals("30") || id.get(index).equals("31") || id.get(index).equals("15")) {
					//orの場合は一時的に異なる型になる可能性がある
					//例) i or b < 7:
					if(id.get(index).equals("15")) {
						roop_flag = 0;
					}

					//足算があることを管理
					if(id.get(index).equals("30")) {
						add_ope_flag = 1;
						//負数の時
						negative_cas();
					}
					//引き算があることを管理
					else if(id.get(index).equals("31")) {
						sub_ope_flag = 1;
						//負数の時
						negative_cas();
					}
					else if(id.get(index).equals("15")) {
						or_ope_flag = 1;
					}

					index ++;
					continue;
				}
				else {
					break;
				}
			}

			//関係演算子のcasコードを書き出す
			if(equals_flag ==1 ) {
				write_rerational_ope();
				main_part.append("\t" + "JZE" + "\t" + "TRUE" + String.valueOf(while_count) + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("\t" + "JUMP" + "\t" + "BOTH" +  String.valueOf(while_count) + "\n");
				main_part.append("TRUE" +  String.valueOf(while_count) + "\t" + "LD" + "\t" + "GR1, =#0000" + "\n");
				main_part.append("BOTH" +  String.valueOf(while_count) + "\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				equals_flag = 0;
				while_count ++;
				negative_counter = 0;
			}
			else if(not_equals_flag == 1) {
				write_rerational_ope();
				main_part.append("\t" + "JNZ" + "\t" + "TRUE" + String.valueOf(while_count) + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("\t" + "JUMP" + "\t" + "BOTH" +  String.valueOf(while_count) + "\n");
				main_part.append("TRUE" +  String.valueOf(while_count) + "\t" + "LD" + "\t" + "GR1, =#0000" + "\n");
				main_part.append("BOTH" +  String.valueOf(while_count) + "\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				not_equals_flag = 0;
				while_count ++;
				negative_counter = 0;
			}
			else if(less_than_flag == 1) {
				write_rerational_ope();
				main_part.append("\t" + "JMI" + "\t" + "TRUE" + String.valueOf(while_count) + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("\t" + "JUMP" + "\t" + "BOTH" +  String.valueOf(while_count) + "\n");
				main_part.append("TRUE" +  String.valueOf(while_count) + "\t" + "LD" + "\t" + "GR1, =#0000" + "\n");
				main_part.append("BOTH" +  String.valueOf(while_count) + "\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				less_than_flag =0;
				while_count ++;
				negative_counter = 0;
			}
			else if(or_less_flag == 1) {
				write_rerational_ope();
				main_part.append("\t" + "JPL" + "\t" + "TRUE" + String.valueOf(while_count) + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =#0000" + "\n");
				main_part.append("\t" + "JUMP" + "\t" + "BOTH" +  String.valueOf(while_count) + "\n");
				main_part.append("TRUE" +  String.valueOf(while_count) + "\t" + "LD" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("BOTH" +  String.valueOf(while_count) + "\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				or_less_flag = 0;
				while_count ++;
				negative_counter = 0;
			}
			else if(or_more_flag == 1) {

				write_rerational_ope();
				main_part.append("\t" + "JMI" + "\t" + "TRUE" + String.valueOf(while_count) + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =#0000" + "\n");
				main_part.append("\t" + "JUMP" + "\t" + "BOTH" +  String.valueOf(while_count) + "\n");
				main_part.append("TRUE" +  String.valueOf(while_count) + "\t" + "LD" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("BOTH" +  String.valueOf(while_count) + "\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				or_more_flag = 0;
				while_count ++;
				negative_counter = 0;
			}
			else if(more_than_flag == 1){
				write_rerational_ope();
				main_part.append("\t" + "JPL" + "\t" + "TRUE" + String.valueOf(while_count) + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =#FFFF" + "\n");
				main_part.append("\t" + "JUMP" + "\t" + "BOTH" +  String.valueOf(while_count) + "\n");
				main_part.append("TRUE" +  String.valueOf(while_count) + "\t" + "LD" + "\t" + "GR1, =#0000" + "\n");
				main_part.append("BOTH" +  String.valueOf(while_count) + "\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				more_than_flag = 0;
				while_count ++;
				negative_counter = 0;
			}
			//関係演算子

			if(id.get(index).equals("24") || id.get(index).equals("25") || id.get(index).equals("26")
					|| id.get(index).equals("27") || id.get(index).equals("28")|| id.get(index).equals("29")) {
				//負数の時
				negative_cas();

				if(id.get(index).equals("24") || id.get(index).equals("25")) {
					roop_flag = 0;
				}
				//=
				if(id.get(index).equals("24")) {
					equals_flag = 1;
				}
				//<>
				else if(id.get(index).equals("25")){
					not_equals_flag = 1;
				}
				//<
				else if(id.get(index).equals("26")) {
					less_than_flag = 1;
				}
				//<=
				else if(id.get(index).equals("27")) {
					or_less_flag = 1;
				}
				//>=
				else if(id.get(index).equals("28")) {
					or_more_flag = 1;
				}
				//>
				else if(id.get(index).equals("29")){
					more_than_flag = 1;
				}

				relational_ope_flag = 1;
				index ++;
				continue;

			}
			else {
				break;
			}


		}
		//負数の時
		negative_cas();


		//条件式から呼び出された時
		if(conditional_check ==1 && call_by_factor_flag ==0) {

			if(! (data_type_of_fomula.equals("3") || relational_ope_flag == 1) ) {
				return "Semantic error: line " + line.get(index);
			}
			relational_ope_flag = 0;
		}

		//代入分から呼び出された時
		if(subsituation_check == 1 && call_by_factor_flag == 0) {
			//boolean
			if((data_type_of_fomula.equals("3") || relational_ope_flag == 1)) {
				data_type_of_fomula = "3";
			}

			//代入分の左辺と右辺の型チェック
			if( ! subsituation_type.equals(data_type_of_fomula)) {
				return "Semantic error: line " + line.get(index);
			}
		}



		roop_flag = 0;
		gr_index = index;
		return "0";

	}

	public String is_factor(ArrayList<String> id, ArrayList<String> line, int index) {
		String check = null;
		int not_flag = 0;//not 因子の時にbooleanかどうかを判定]
		int check_string = 0;
		int check_call_by_sub_fomula = 0;
		int num_variable = 0;
		int array_flag = 0;
		String data_type;

		if(call_by_writeln == 1) {
			check_string = 1;
			call_by_writeln = 0;
		}
		//代入分から呼び出された式から呼び出されたことを管理
		if(call_by_sub_fomula == 1) {
			check_call_by_sub_fomula = 1;
			call_by_sub_fomula = 0;
		}

		//変数
		if(id.get(index).equals("43")) {

			//変数が存在するかチェック
			if(is_block_has_var(index)) {
				Variable_table fact_variable = code_block[variable_place].var_search(get_name_list.get(index));
				data_type = fact_variable.data_type;

				//添字の場合 integerかどうかを判定する
				if(sufix_flag ==1) {
					//integer 11
					if(! data_type.equals("11")) {
						return "Semantic error: line " + line.get(index);
					}
				}
				//変数のデータ型を保存しておく(型検査)
				data_type_of_fac = data_type;
				num_variable = fact_variable.num_of_var;
			}
			else {
				return "Semantic error: line " + line.get(index);
			}


			index ++;

			//添字月変数の場合
			//[
			if(id.get(index).equals("35")) {
				index ++;
				//配列処理を管理するフラッグ
				array_flag = 1;
				//式
				int flag = negative_flag;
				negative_flag = 0;
				call_by_factor_flag = 1;
				check = is_fomula(id,line,index);
				call_by_factor_flag = 0;
				negative_flag = flag;
				if(! check.equals("0")) { return check; }

				index = gr_index;


				//]
				if(! id.get(index).equals("36")) { return "Syntax error: line " +line.get(index); }
				index ++;
			}

			//変数出力のcasコードを書き込み
			if(array_flag == 1) {
				main_part.append("\t" + "POP" + "\t" +" GR2" + "\n");
				if(num_variable > 1) {
					main_part.append("\t" + "ADDA" + "\t" +" GR2, ="+ String.valueOf(num_variable-2) + "\n");
				}
				else if(num_variable == 1) {
					main_part.append("\t" + "ADDA" + "\t" +" GR2, ="+ String.valueOf(num_variable-1) + "\n");
					main_part.append("\t" + "SUBA" + "\t" +" GR2, ="+ 1 + "\n");
				}
			}
			//配列じゃない
			else if(array_flag == 0) {
				main_part.append("\t" + "LD" + "\t" +" GR2, " + "=" + String.valueOf(num_variable-1) + "\n");
			}
			//変数のcasファイル書き出し
			set_variable();

			//整数書き出しのcasコード
			if(check_string == 1 && data_type.equals("11")) {
				write_int_flag = 1;
			}
			else if(check_string == 1 && data_type.equals("4")) {
				write_char_flag = 1;
			}

		}
		//定数
		//符合なし整数、文字列、false、true
		else if(id.get(index).equals("44") || id.get(index).equals("45")|| id.get(index).equals("9") || id.get(index).equals("20")) {
			//添字の場合 integerかどうかを判定する
			if(sufix_flag ==1) {
				//integer 11
				if(! id.get(index).equals("44")) {
					return "Semantic error: line " + line.get(index);
				}
			}

			//writelnから呼び出された時の文字列
			if(check_string == 1 && id.get(index).equals("45")) {
				write_str_cas(index);
			}

			//データ型の格納(型検査)
			//integer
			if(id.get(index).equals("44")) {
				data_type_of_fac = "11";
				//代入分から呼び出された式からの呼び出しの場合
				//数字の時
				//casファイル生成
				//if(check_call_by_sub_fomula == 1) {
				main_part.append("\t" + "PUSH" + "\t" + get_name_list.get(index) + "\n");

				//}
			}
			//char
			else if(id.get(index).equals("45")) {
				if(check_call_by_sub_fomula == 1 || call_by_if == 1) {
					main_part.append("\t" + "LD" + "\t" + "GR1, =" + get_name_list.get(index) + "\n");
					main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
				}
				data_type_of_fac = "4";
			}
			// true or false
			else {
				//false
				if(id.get(index).equals("9") ) {
					main_part.append("\t" + "PUSH" + "\t" + "#FFFF" + "\n");
				}
				else if(id.get(index).equals("20")) {
					main_part.append("\t" + "PUSH" + "\t" + "#0000" + "\n");
				}
				data_type_of_fac = "3";
			}
			index ++;
			//System.out.println("akakak");
		}
		//式
		// (
		else if(id.get(index).equals("33")) {
			index ++;

			fomula_flag = 1;
			call_by_factor_flag = 1;
			int negative_cas = 0;
			if(negative_flag == 1) {
				negative_cas = 1;
				negative_flag = 0;
			}
			//式
			check = is_fomula(id,line,index);
			if(! check.equals("0")) { return check;}
			index = gr_index;

			call_by_factor_flag = 0;
			fomula_flag = 0;

			//)
			if(! id.get(index).equals("34")) { return "Syntax error: line " +line.get(index); }
			index ++;
			if(negative_cas == 1) {
				main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
				main_part.append("\t" + "LD" + "\t" + "GR1, =0" + "\n");
				main_part.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\n");
				main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
			}

		}
		//not 因子
		else if(id.get(index).equals("13")) {
			not_flag = 1;
			index ++;

			//因子
			check = is_factor(id,line,index);
			if(! check.equals("0")) { return check; }
			//反転not
			main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
			main_part.append("\t" + "XOR" + "\t" + "GR1, =#FFFF" + "\n");
			main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
			index = gr_index;
			not_flag = 0;
		}
		else {
			return "Syntax error: line " +line.get(index);
		}

		gr_index = index;
		return "0";
	}

    //重複
	//宣言されていない
	//配列の名前じゃないか

	//重複チェック
	public boolean sematic_check_double_vriable(String check_variable_name, LinkedList code_block) {
		//変数名がすでに宣言されているかを調べる
		return code_block.search(check_variable_name);
	}

	//変数が存在するか全てのブロックで調べる
	public boolean is_block_has_var(int index) {
		//コードブロック全てを調べる
		for(int i= code_block_num; i >= 0; i--) {

			if(code_block_state[i] == 1 && code_block[i].search(get_name_list.get(index))) {
				variable_place = i;
				return true;

			}

		}
		return false;
	}

	//副プログラムが存在するか判定する
	public boolean is_block_has_procedure_call(int index) {
		for(int i= code_block_num; i >= 0; i--) {

			if(code_block[i].search(get_name_list.get(index))) {
				variable_place = i;
				return true;

			}

		}
		return false;
	}

	//何番目の変数かどうかを調べるメソッド
	public int what_num_is_var(int index) {
		//コードブロック全てを調べる
		for(int i= code_block_num; i >= 0; i--) {
			if(code_block_state[i] == 1 && code_block[i].search(get_name_list.get(index))) {
				variable_place = i;
			}
		}

		Variable_table variable = code_block[variable_place].var_search(get_name_list.get(index));
		//変数の順番を返す
		return variable.num_of_var;
	}

	public void write_str_cas(int index) {
		String str_name = "CHAR" + String.valueOf(str_counter);
		String str_length = String.valueOf(get_name_list.get(index).length() -2);

		//文字列出力のcasコードを書き込む
		main_part.append("\t" + "LD" + "\t" +" GR1, " + "=" + str_length + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
		main_part.append("\t" + "LAD" + "\t" + "GR2, " + str_name + "\n");
		main_part.append("\t" + "PUSH" +"\t" + "0, GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "CALL" + "\t" + "WRTSTR" + "\n");


		//casファイル宣言部部分へ追加する
		declear_part.append(str_name + "\t" + "DC" + "\t" + get_name_list.get(index) + "\n");
		str_counter ++ ;
	}

	//整数の書き出し
	public void write_int_cas() {
		main_part.append("\t" + "POP" +"\t" + "GR2" + "\n");
		main_part.append("\t" + "CALL" + "\t" + "WRTINT" + "\n");

	}

	public void write_char_cas() {
		main_part.append("\t" + "POP" +"\t" + "GR2" + "\n");
		main_part.append("\t" + "CALL" + "\t" + "WRTCH" + "\n");

	}

	//整数の代入
	public void sub_left(int array_flag) {
		//main_part.append("\t" + "PUSH" + "\t" + value + "\n");
		//main_part.append("\t" + "LD" + "\t" + "GR2, ="+ String.valueOf(num_of_variable)  + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		if(array_flag == 1) {
			main_part.append("\t" + "ST" +"\t" + "GR1, VAR, GR3" + "\n");
		}
		else {
			main_part.append("\t" + "ST" +"\t" + "GR1, VAR, GR2" + "\n");
		}
	}

	//変数をスタックへセット
	public void set_variable() {
		//main_part.append("\t" + "LD" + "\t" +" GR2, " + "=" + String.valueOf(num_of_variable) + "\n");
		main_part.append("\t" + "LD" + "\t" + "GR1, VAR, GR2" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");

	}

	//足算のcasコード
	public void write_add_cas() {
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "ADDA" + "\t" + "GR1, GR2" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
	}

	//引き算のcasコード
	public void write_sub_cas() {
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
	}
	//掛け算のcasコード
	public void write_mul_cas() {
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		//負数判定
		//main_part.append("\t" + "CPL" + "\t"+ "GR2,=0" + "\n");


		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "CALL" + "\t" + "MULT" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR2" + "\n");

	}
	//割り算のcasコード
	public void write_div_cas() {
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "CALL" + "\t" + "DIV" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR2" + "\n");
	}

	//modのcasコード
		public void write_mod_cas() {
			main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
			main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
			main_part.append("\t" + "CALL" + "\t" + "DIV" + "\n");
			main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
		}

	public void negative_cas() {
		if(negative_flag == 1) {
			main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
			main_part.append("\t" + "LD" + "\t" + "GR1, =0" + "\n");
			main_part.append("\t" + "SUBA" + "\t" + "GR1, GR2" + "\n");
			main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
			negative_flag = 0;
		}
	}

	public void write_and_cas() {
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "AND" + "\t" + "GR1, GR2" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
	}

	public void write_or_cas() {
		main_part.append("\t" + "POP" + "\t" + "GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" + "GR1" + "\n");
		main_part.append("\t" + "OR" + "\t" + "GR1, GR2" + "\n");
		main_part.append("\t" + "PUSH" + "\t" + "0, GR1" + "\n");
	}

	public void write_rerational_ope() {
		main_part.append("\t" + "POP" + "\t" +" GR2" + "\n");
		main_part.append("\t" + "POP" + "\t" +" GR1" + "\n");
		main_part.append("\t" + "CPA" + "\t" +" GR1, GR2" + "\n");
	}







}
