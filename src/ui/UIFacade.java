package ui;

import java.io.FileNotFoundException;
import java.util.Scanner;

import facade.GerenciadorFacade;
import negocios.AgentePrevisao;
import negocios.PlanoContas;
import util.CategoriaMes;
import util.UIHelpers;

/**
 *  Classe responsável por conter os métodos que a UI utiliza, porém somente métodos que interagem com usuário. 
 *  É uma fachada, que encapsula os métodos de interação com o usuário e os gerencia no método uiFacade();
 *
 */

public class UIFacade {
	
	private UIHelpers uiHelper;
	PlanoContas planoContas;
	GerenciadorFacade facade;
	private boolean executing;
	private boolean fileExists;
	private boolean dateIsValid;
	private boolean optionValid;
	private boolean predictionContinue;
	
	public UIFacade() {
		 planoContas = PlanoContas.getInstance();
		 facade = new GerenciadorFacade(planoContas);
		 uiHelper=  UIHelpers.getInstance(facade);
		 executing = true;
		 fileExists = false;
		 dateIsValid = false;
		 optionValid = false;
		 predictionContinue = true;
	}

	public void uiFacade() {
		
		while(this.executing) {
			
			//Requisição do plano de contas inicial
			while(!this.fileExists) {
				this.fileExists = getFileExistance();
			}
			while(!this.dateIsValid) {
				this.dateIsValid = verificaValidadeDataDeCongelamento();
			}
			
			while(!this.optionValid) {
				int predicitionOp = validOption();
				if(predicitionOp != -1) {
					do {
						predictionValue(predicitionOp);
						this.predictionContinue = predictionEnd();
						System.out.println(this.predictionContinue); //--> TA DANDO FALSE MAS N SAI
					}while(this.predictionContinue);
				}
			}
			geraAnaliseComparativa();
			
		}
		//this.uiHelper.stubFunction();  -> Porque DIABOS, se usa stub da ui? Tem de fazer uma classe de testes para UI e colocar  stub lá, mas já perdi tanto tempo refatorando a UI que nao to com saco de fazer testes de UI agr
	}
	
	private boolean getFileExistance() {
		String csvName = this.askUser("Digite o nome do arquivo que se deseja ler(.csv)(0 para sair)", "0");
		try {
			facade.lerOrcamentoInicial(csvName);
			return  true;
		}catch(FileNotFoundException e) {
			System.out.println("Arquivo não encontrado!\n");
			return false;
		}
	}
	
	private String askUser(String question, String quitOp) {
		Scanner inputChannel = new Scanner(System.in);
		System.out.println(question+"\n");
		String input = inputChannel.nextLine();
		if(input.equals(quitOp) && !quitOp.equals("")) {
			this.executing = false;
			System.out.println("Obrigada por utilizar o sistema!");
			System.exit(-1);
		}
		return input;		
	}
	private boolean verificaValidadeDataDeCongelamento() {
		int day, month, year;
		System.out.println("Digite a data de congelamento(dd/mm/yyyy)(0 para sair)");
		day = Integer.valueOf(this.askUser("Dia", "0"));
		month = Integer.valueOf(this.askUser("Mês", "0"));
		year = Integer.valueOf(this.askUser("Ano", "0"));
		return this.uiHelper.returnIfIsAValidDate(day, month, year);
	}
	
	private int validOption() {
		int predicitionOp = Integer.valueOf(askUser("Digite a opção para a previsão: 1-Valor Fixo "
                + "  2-Valor Porcentagem   3-Manter Valor Ano Anterior (0 para sair)", "0"));
		
		if(predicitionOp < 1 || predicitionOp > 3) {
			System.out.println("Opção não reconhecida!!");
			return -1;
		}
		
		else {
			return predicitionOp;
		}
	}
	private void predictionValue(int predicitionOp) {
		int rubricaCode = Integer.valueOf(this.askUser("Digite o código da Rubrica que desejas prever o valor", ""));
		int predictionMonth = Integer.valueOf(this.askUser("Digite o mes de previsão da Rubrica que desejas prever o valor", ""));
		int predictionValue;
		if(predicitionOp == AgentePrevisao.PREVISAO_VALORANOANTERIOR) {
			predictionValue = 0;
		}
		else if(predicitionOp == AgentePrevisao.PREVISAO_VALORFIXO){
			predictionValue = Integer.valueOf(this.askUser("Digite o valor de alteração da Rubrica que desejas prever o valor", ""));
		}
		else {
			predictionValue = Integer.valueOf(this.askUser("Digite o valor percentual de alteração da rubrica", ""));
		}
	
		this.uiHelper.generatePrediction(predicitionOp , rubricaCode , predictionValue,predictionMonth);
		
	}
	private boolean predictionEnd() {
		String predicitionContinues = this.askUser("Desejas continuar a previsão?(s/n)", "");
		if(predicitionContinues.equals("n")) {
			return false;
		}
		else {
			return true;
		}
	}
	
	private void geraAnaliseComparativa() {
		this.askUser("Queres gerar analise comparativa? N para nao", "N");
		int mesInicial = Integer.valueOf(this.askUser("Digite o mes inicial! 1 - janeiro. 12 - dezembro", ""));
		int mesFinal = Integer.valueOf(this.askUser("Digite o mes final! 1 - janeiro. 12 - dezembro", ""));
		facade.geraAnalise(CategoriaMes.values()[mesInicial - 1], CategoriaMes.values()[mesFinal - 1]  );
	}

	
}