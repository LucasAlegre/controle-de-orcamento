package negocios;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import drivers.DriverCSV;
import util.CategoriaMes;

public class GerenciadorArquivos {

	
	public GerenciadorArquivos(){
		
	}

	public LinkedHashMap<Integer, Rubrica> lerOrcamentoInicial(String filename){
	
		
		LinkedHashMap<Integer, Rubrica> map = new LinkedHashMap<Integer, Rubrica>();
			
		//Lista de pais para a estruturação da linhagem hierárquica
		List<Rubrica> pais = new ArrayList<>();
		Rubrica pai = null;
		int classPai = 0;
		
		try {
			
			DriverCSV driver = new DriverCSV(filename);
			
			Double[] valoresPassadosMensal;
			
			while(driver.hasNext()) {
				Rubrica rubrica;
				// INTERPRETAÇÃO DA LINHA
				driver.proceed();
				
				if(driver.getNumOfLineFields() != 0) {
					
					String classe = driver.getFields()[0];
					String cod = driver.getFields()[1];
					String nome = driver.getFields()[2];
					
					//Caso a rúbrica for classificável
					if(!classe.equals("")) {
						
						valoresPassadosMensal = new Double[driver.getNumOfLineFields()-3];
						
						for(int i = 3; i<driver.getNumOfLineFields(); i++) {
							valoresPassadosMensal[i-3] = Double.valueOf(driver.getFields()[i]);
						}
						//Instanciar o número de pontos de classificação da rúbrica,
						//ou seja, verificar em que nível a rúbrica está
						int pontos = classe.length() - classe.replace(".", "").length();
						
						//Caso o número de pontos seja zero, ou seja, a rubrica lida não possui pai
						//limpa-se os pais registrados e inicializa a rubrica com pai=null
						if(pontos == 0) {
							pais.clear();
							rubrica = new Rubrica(null, nome, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, valoresPassadosMensal);
						
						//Caso a rubrica esteja classificada ao mesmo nível de sua antecessora, 
						//possui o mesmo pai que ela
						}
						else if(pontos == classPai) {
							rubrica = new Rubrica(pai.getPai(), nome, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, valoresPassadosMensal);
							pai.getPai().addSubRubrica(rubrica);
							
							
						//Caso a rubrica esteja em um nível mais profundo que a outra, adicionamos a rubrica
						//anterior na lista de pais a adicionamos como pai da rubrica atual
						}
						else if(pontos > classPai){
							rubrica = new Rubrica(pai, nome, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, valoresPassadosMensal);
							if(pai != null) {
								pais.add(pai);
								pai.addSubRubrica(rubrica);
							}
						
						//Caso tenhamos voltado um nível, deletamos a diferença de níveis da rubrica atual e da anterior,
						//assim como retomamos a posição da rubrica antes de intanciarmos ela
						}
						else {
							try{
								pai = pais.get(pontos - 1);
							}catch(IndexOutOfBoundsException e){
								pai =  null;
								pais.clear();
							}
							rubrica = new Rubrica(pai, nome, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, valoresPassadosMensal);
							if(pai != null)pai.addSubRubrica(rubrica);
							for(int i = pontos; i < pais.size(); i++) {
								pais.remove(i);
							}
						}
						//set final
						pai = rubrica;
						classPai = pontos;
							
						//coloca no mapa
						map.put(Integer.valueOf(cod), rubrica);
					}
				}
				
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Arquivo " + filename + " não encontrado.");
		}
		
		return map;
	}
	
	public LinkedHashMap<Integer, Double> lerRealizadoMensal(String filename){
		
		LinkedHashMap<Integer, Double> realizados = new LinkedHashMap<Integer, Double>();
		
		try {
			InputStream inp = new FileInputStream(filename);
			Workbook wb = WorkbookFactory.create(inp);
			Sheet sheet = wb.getSheetAt(0);
			
			int row = 2;
			while(sheet.getRow(row) != null) {
				
				Row linha = sheet.getRow(row);
				int codigo = (int) linha.getCell(1).getNumericCellValue();
				double debito = linha.getCell(2).getNumericCellValue();
				double credito = linha.getCell(3).getNumericCellValue();
				
				realizados.put(codigo, credito - debito);
				
				row++;
			}
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(InvalidFormatException e) {
			e.printStackTrace();
		} catch (EncryptedDocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return realizados;
	}
	
	/**
	 * Dado um plano de contas e um mes, gera um tamplate de arquivo .xls para o realizado mensal
	 * 
	 * @param planoContas
	 * @param mes
	 */
	public void geraTemplateRealizadoMensal(PlanoContas planoContas, CategoriaMes mes) {
		
		 try {
	           
	            HSSFWorkbook workbook = new HSSFWorkbook();
	            HSSFSheet sheet = workbook.createSheet("FirstSheet"); 
	            sheet.setColumnWidth(0, 10000);

	            HSSFRow mesRelizado = sheet.createRow((short)0);
	            mesRelizado.createCell(0).setCellValue(mes.toString());
	            
	            
	            HSSFRow rowhead = sheet.createRow((short)1);
	            rowhead.createCell(0).setCellValue("Descrição da conta");
	            rowhead.createCell(1).setCellValue("Código");
	            rowhead.createCell(2).setCellValue("Débito");
	            rowhead.createCell(3).setCellValue("Crédito");
	            
	            int cont = 2;
	            for (Integer key : planoContas.getRubricas().keySet()) {
	            	
            		HSSFRow newRubricaRow = sheet.createRow((short)cont);
            
            		HSSFCell rubricaNameCell = newRubricaRow.createCell(0);
            		rubricaNameCell.setCellValue(planoContas.getRubricas().get(key).getNome());
           
        			newRubricaRow.createCell(1).setCellValue(key);
        			
		            cont++;
	            }

	            String outputFileName = "Template" + mes.toString() + ".xls";
	            FileOutputStream fileOut = new FileOutputStream(outputFileName);
	            workbook.write(fileOut);
	            fileOut.close();
	            System.out.println("Arquivo " + outputFileName + " gerado!");
	            workbook.close();

	        } catch ( Exception ex ) {
	            System.out.println(ex);
	        }    
	}

	
	//TODO: Gerar um arquivo por m�s. Lembrar que dessa maneira q foi feito, como � sempre o mesmo nome de arquivo, ele cria um,e qaundo manda o m�todo rodar dnv ele sobreescreve o arquivo de antes
	//� �timo pra testar pq n fica mil arquivos, mas qnd for pra finalizar o trabalho vamos ter de ter um controle de cada arquivo ter um nome diferente
	//uma ideia que eu tive � tipo, faz o janeiro funcionar, se ele funcionar s� mete um for fora de tudo dando append de um contador nos nomes dos arquivos, dai 
	//ele gera os 12 meses!
	public void geraArquivoPrevisoes(PlanoContas planoContas, String filename) {
		 
		 
		 try {
	           
	            HSSFWorkbook workbook = new HSSFWorkbook();
	            HSSFSheet sheet = workbook.createSheet("FirstSheet");
	            sheet.setColumnWidth(0, 10000);
	            
	            HSSFCellStyle cellStyle = workbook.createCellStyle();
	            cellStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
	            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	            
	            HSSFCellStyle cellHeaderStyle = workbook.createCellStyle();
	            cellHeaderStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
	            cellHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	            
	            HSSFRow rowhead = sheet.createRow((short)0);          
	            
	            HSSFCell newcell = rowhead.createCell(0);
	            newcell.setCellStyle(cellHeaderStyle);
	            newcell.setCellValue("Rubrica");
	            newcell = rowhead.createCell(1);
	            newcell.setCellStyle(cellHeaderStyle);
	            newcell.setCellValue("Codigo");
    
	            for (int mesIndex = 0; mesIndex < 12; mesIndex ++) {
            		newcell = rowhead.createCell(mesIndex + 2);
            		newcell.setCellStyle(cellHeaderStyle);
            		newcell.setCellValue(CategoriaMes.values()[mesIndex].toString());	
	            }
    
	            int cont = 0;
	            for (Integer key : planoContas.getRubricas().keySet()) {
	            		HSSFRow newRubricaRow = sheet.createRow((short)cont+1);
	            		HSSFCell rubricaNameCell = newRubricaRow.createCell(0);
	            		rubricaNameCell.setCellValue(planoContas.getRubricas().get(key).getNome());
	            		rubricaNameCell.setCellStyle(cellStyle);
            			newRubricaRow.createCell(1).setCellValue(key);
		            this.fillRubricasMonths(planoContas.getRubricas().get(key), newRubricaRow);
		            cont ++;
	            }
	            
	            FileOutputStream fileOut = new FileOutputStream(filename);
	            workbook.write(fileOut);
	            fileOut.close();
	            System.out.println("Arquivo " + filename +  " gerado!");
	            workbook.close();
	        } catch ( Exception ex ) {
	            System.out.println(ex);
	        }    
	}
	
	private void fillRubricasMonths (Rubrica rubrica, HSSFRow rubricaRow) {
		for (int cont = 0; cont < 12; cont ++) {
			try {
			//if (rubrica.getValorPrevisto(cont) != null) {
	            rubricaRow.createCell(cont + 2).setCellValue(rubrica.getValorPrevisto(cont));	
			}
			catch (NullPointerException exc) {
	            rubricaRow.createCell(cont + 2).setCellValue("-");	
			}
        }
		
	}
	
	
	public static void main(String Args[]) {
		
		GerenciadorArquivos le = new GerenciadorArquivos();
		System.out.println(le.lerOrcamentoInicial("Modelo_Controle_Orcamentario_Completo.csv").toString());

	}
	
}