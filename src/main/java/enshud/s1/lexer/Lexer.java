package enshud.s1.lexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class Lexer {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Lexer().run("data/pas/normal01.pas", "tmp/out1.ts");
		new Lexer().run("data/pas/normal02.pas", "tmp/out2.ts");
		new Lexer().run("data/pas/normal03.pas", "tmp/out3.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるLexer実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたpasファイルを読み込み，トークン列に分割する．
	 * トークン列は第二引数で指定されたtsファイルに書き出すこと．
	 * 正常に処理が終了した場合は標準出力に"OK"を，
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力pasファイル名
	 * @param outputFileName 出力tsファイル名
	 */
	public void run(final String inputFileName, final String outputFileName) {

		final String[] token_source = {"and", "array", "begin", "boolean", "char", "div", "/", "do", "else", "end", "false", "if",
									"integer", "mod", "not", "of", "or", "procedure","program", "readln", "then","true", "var", "while",
									"writeln", "=", "<>", "<", "<=", ">=", ">", "+", "-", "*", "(", ")", "[", "]", ";", ":", "..",":=",
									",", "."};

		final String[] token = {"SAND","SARRAY","SBEGIN","SBOOLEAN","SCHAR","SDIVD","SDIVD","SDO","SELSE","SEND","SFALSE"
				,"SIF","SINTEGER","SMOD","SNOT","SOF","SOR","SPROCEDURE","SPROGRAM","SREADLN","STHEN","STRUE","SVAR","SWHILE","SWRITELN"
				,"SEQUAL","SNOTEQUAL","SLESS","SLESSEQUAL","SGREATEQUAL","SGREAT","SPLUS","SMINUS","SSTAR","SLPAREN","SRPAREN"
				,"SLBRACKET","SRBRACKET","SSEMICOLON","SCOLON","SRANGE","SASSIGN","SCOMMA","SDOT"};

		int line_count = 0;  //行数カウント
		int alpha_flag = 0;
		int num_flag = 0;
		int annotation_flag = 0;
		int symbol_flag = 0;
		int string_flag = 0;
		int fin = 0;
		int token_num = 0;
		String[] check_token = new String[100];//トークンの長さ制限を100文字とする
		// TODO
		try(BufferedReader in = new BufferedReader(new FileReader(new File(inputFileName)))){  //ファイル読み込み

			FileWriter file = new FileWriter(outputFileName);
			PrintWriter pw = new PrintWriter(new BufferedWriter(file));

			String line;
			while((line = in.readLine()) != null) {
				String[] input_line_char = new String[line.length()];  //読み込み文字列を1文字ずつ分割して一時保管用する配列
																	//初期化いる？
				for(int i = 0; i < line.length(); i ++) {           //読み込んだ文字列を1文字ずつに分解する
					input_line_char[i] = String.valueOf(line.charAt(i));
				}
				line_count ++ ;                                     //現在の行数をカウントする

				for(int char_num = 0; char_num < input_line_char.length; char_num ++) {//分割した文字を1文字ずつ見ていく


					//１文字目の場合
					if(alpha_flag == 0 && num_flag == 0 && annotation_flag == 0 && symbol_flag == 0 && string_flag ==0) {
						token_num = 0;

						if(input_line_char[char_num].matches("[a-zA-Z]")) {          //１文字目が文字列のとき
							alpha_flag = 1;
							check_token[token_num] = input_line_char[char_num];
							token_num ++;
						}
						else if(input_line_char[char_num].matches("[0-9]")){          //１文字目が数字の時
							num_flag = 1;
							check_token[token_num] = input_line_char[char_num];
							token_num ++;
						}
						else if(input_line_char[char_num].equals("{")) {             //１文字目が注釈
							annotation_flag = 1;
						}
						else if(input_line_char[char_num].equals("'")) {             //文字列
							string_flag = 1;
							check_token[token_num] = input_line_char[char_num];
							token_num ++;

						}
						else if(input_line_char[char_num].equals(" ") || input_line_char[char_num].equals("\t")) {//読み飛ばす

						}
						else {                                                        //記号
							symbol_flag = 1;
							check_token[token_num] = input_line_char[char_num];
							token_num ++;
						}

					}




					//２文字目以降を見る
					if(char_num >= input_line_char.length -1) {//行の最後の文字かどうかを判定

						fin = 1;
						alpha_flag = 0;
						symbol_flag = 0;
						annotation_flag = 0;

					}
					else {                                       //行の最後でない場合

						//1文字目がアルファベット
						//次の文字がトークンの一部かどうかを判定する
						if(alpha_flag == 1) {

							if(input_line_char[char_num + 1].matches("[a-zA-Z]") || input_line_char[char_num +1].matches("[0-9]")){
								check_token[token_num] = input_line_char[char_num + 1];
								token_num ++;
							}
							else {
								alpha_flag = 0;
								fin = 1;
							}

						}
						//１文字目が数字
						//次の文字がトークンの一部かどうかを判定
						else if(num_flag == 1){

							if(input_line_char[char_num + 1].matches("[0-9]")) {
								check_token[token_num] = input_line_char[char_num + 1];
								token_num ++;
							}
							else {
								fin = 1;
							}

						}
						//１文字目が記号
						//次の文字がトークンの一部かどうかを判定
						else if( symbol_flag == 1) {

							if( check_token[0].equals("<") && input_line_char[char_num + 1].equals(">")     ||
								check_token[0].equals(":") && input_line_char[char_num + 1].equals("=") ||
								check_token[0].equals(".") && input_line_char[char_num + 1].equals(".") ||
								check_token[0].equals("<") && input_line_char[char_num + 1].equals("=") ||
								check_token[0].equals(">") && input_line_char[char_num + 1].equals("=")) {

								check_token[token_num] = input_line_char[char_num + 1];
								token_num ++ ;
								symbol_flag = 0;
								fin = 1;
								char_num ++ ;
							}
							else {
								symbol_flag = 0;
								fin = 1;
							}

						}
						//注釈
						else if(annotation_flag == 1) {

							if(input_line_char[char_num + 1].equals("}")) {
								annotation_flag = 0;
								char_num ++;
							}

						}
						//文字列
						else if(string_flag == 1) {

							if(input_line_char[char_num + 1].equals("'")) {
								char_num ++;
								check_token[token_num] = input_line_char[char_num];
								fin = 1;
							}
							else {
								check_token[token_num] = input_line_char[char_num + 1];
								token_num ++;
							}

						}

					}



					//終了
					//ファイルへ出力する
					if(fin == 1) {

						//文字列の結合
						 StringBuilder judge = new StringBuilder(check_token[0]);
						 for(int j = 1; check_token[j] != null; j++) {
							 judge.append(check_token[j]);
						 }

						 String j_token = judge.toString();
						 int symbol = 0;
						 int token_number = 0;
						 int token_id = 0;

						 //デフォルトトークンかどうかを判定する
						 for(int i = 0; i < token_source.length; i++) {
							 if(token_source[i].equals(j_token)) {
								 symbol = 1;
								 if(i > 5) {
									 token_id = i - 1;
								 }
								 else {
									 token_id = i;
								 }
								 token_number = i;
								 break;
							 }
						 }


						 //ファイルへ書き出し
						if(symbol == 1) {
							pw.println(j_token + "\t" + token[token_number] + "\t" + token_id + "\t" + line_count );
							symbol = 0;
						}
						else if(string_flag == 1) {
							pw.println(j_token + "\t" + "SSTRING" + "\t" + "45" + "\t"+ line_count);
							string_flag = 0;
						}
						else if(num_flag == 1) {
							pw.println(j_token + "\t" + "SCONSTANT" + "\t" + "44" + "\t" + line_count );
							num_flag = 0;
						}
						else {
							pw.println(j_token + "\t" + "SIDENTIFIER" + "\t" + "43" +"\t" + line_count );
						}

						token_num = 0;
						fin = 0;
						Arrays.fill(check_token, null);
					}

				}

			}
			pw.close();
			System.out.println("OK");
		}catch(FileNotFoundException e) {
			//e.printStackTrace();
			System.err.println("File not found");
			//System.exit(-1);
		}catch(IOException e) {
			//e.printStackTrace();
			//System.exit(-1);
			System.err.println("File not found");
		}

	}
}




































