package negocios;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
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
import util.CategoriaAnaliseComparativa;
import util.CategoriaMes;

/**
 *  Classe responsavel pelo acessoa a escrita e leitura dos arquivos da empresa
 *
 */
public class GerenciadorArquivos {

	private HSSFSheet analiseComparativaSheet;
	private HSSFWorkbook analisaComparativaWorkbook;
	
	public GerenciadorArquivos(){
		
	}

	/**
	 * Lê o arquivo de orçamento inicial da empresa
	 * 
	 * @param filename Arquivo .csv com o orçamento inicial
	 * @return Map de Código da Rúbrica -> Rúbrica
	 * @throws FileNotFoundException
	 */
	public LinkedHashMap<Integer, Rubrica> lerOrcamentoInicial(String filename) throws FileNotFoundException{
	
		
		LinkedHashMap<Integer, Rubrica> map = new LinkedHashMap<Integer, Rubrica>();
			
		//Lista de pais para a estruturação da linhagem hierárquica
		List<Rubrica> pais = new ArrayList<>();
		Rubrica parent = null;
		int parentClass = 0;
		
		DriverCSV driver = new DriverCSV(filename);
			
		Double[] pastValues;
		
		while(driver.hasNext()) {
			Rubrica rubrica;
			// INTERPRETAÇÃO DA LINHA
			driver.proceed();
			
			if(driver.getNumOfLineFields() != 0) {
				
				String classification = driver.getFields()[0];
				String cod = driver.getFields()[1];
				String name = driver.getFields()[2];
				
				//Caso a rúbrica for classificável
				if(this.isRubricaValid(driver.getFields())) {
					
					pastValues = this.getPastValues(driver.getFields());
					
					//Instanciar o número de pontos de classificação da rúbrica,
					//ou seja, verificar em que nível a rúbrica está
					int depth = this.getRubricaDepth(driver.getFields());
					
					//Caso o número de pontos seja zero, ou seja, a rubrica lida não possui pai
					//limpa-se os pais registrados e inicializa a rubrica com pai=null
					if(depth == 0) {
						pais.clear();
						rubrica = new Rubrica(null, name, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, pastValues);
					}

					//Caso a rubrica esteja classificada ao mesmo nível de sua antecessora, 
					//possui o mesmo pai que ela
					else if(depth == parentClass) {
						rubrica = new Rubrica(parent.getPai(), name, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, pastValues);
						parent.getPai().addSubRubrica(rubrica);
						
					//Caso a rubrica esteja em um nível mais profundo que a outra, adicionamos a rubrica
					//anterior na lista de pais a adicionamos como pai da rubrica atual
					}
					else if(depth > parentClass){
						rubrica = new Rubrica(parent, name, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, pastValues);
						if(parent != null) {
							pais.add(parent);
							parent.addSubRubrica(rubrica);
						}
					
					//Caso tenhamos voltado um nível, deletamos a diferença de níveis da rubrica atual e da anterior,
					//assim como retomamos a posição da rubrica antes de intanciarmos ela
					}
					else {
						try{
							parent = pais.get(depth - 1);
						}catch(IndexOutOfBoundsException e){
							parent =  null;
							pais.clear();
						}
						rubrica = new Rubrica(parent, name, (int)Integer.valueOf(cod), CategoriaRubrica.DESPESA, pastValues);
						if(parent != null)parent.addSubRubrica(rubrica);
						for(int i = depth; i < pais.size(); i++) {
							pais.remove(i);
						}
					}
					//set final
					parent = rubrica;
					parentClass = depth;
						
					//coloca no mapa
					map.put(Integer.valueOf(cod), rubrica);
				}
			}
				
		}
			
		
		return map;
	}
	
	/**
	 * Verifica a validade de uma linha de rúbrica
	 * @param line Linha do arquivo
	 * @return True se o código da rúbrica não é vazio, falso caso contrário
	 */
	private boolean isRubricaValid(String line[]) {
		String cod = line[1];
		if(cod.equals("")) {
			return false;
		}
		return true;
	}
	
	/**
	 * Lê os valores realizados no ano passado para cada mês
	 * @param line Linha do CSV
	 * @return Array com valor anterior de cada mês
	 */
	private Double[] getPastValues(String line[]) {
		Double pastValues[] = new Double[12];
		int lowerBound = line.length - 12;
		int higherBound = line.length;
		
		for(int i = lowerBound ; i < higherBound; i++) {
			pastValues[i - lowerBound] = Double.valueOf(line[i]);
		}
		
		return pastValues;
	}
	
	
	private int getRubricaDepth(String line[]) {
		String classe = line[0];
		return classe.length() - classe.replace(".", "").length();
	}
	
	/**
	 * Lê um arquivo com os valores realizados em um certo mês.
	 * @param filename
	 * @return Map Código -> Valor Realizado
	 */
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
			
		}catch(EncryptedDocumentException | InvalidFormatException | IOException e) {
			System.out.println("Arquivo " + filename + " inválido");
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
	
	private void criaHeaderAnaliseComparativa(HSSFSheet sheet, HSSFWorkbook workbook) { 
		EnumSet<CategoriaAnaliseComparativa> categorias = EnumSet.allOf(CategoriaAnaliseComparativa.class);
		try {
	        HSSFCellStyle cellHeaderStyle = workbook.createCellStyle();
	        cellHeaderStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
	        cellHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        HSSFRow rowhead = sheet.createRow((short)0);  
	        
	        for (CategoriaAnaliseComparativa categoria : categorias) {
	        		HSSFCell newcell = rowhead.createCell(categoria.toInt());
	            newcell.setCellStyle(cellHeaderStyle);
	            newcell.setCellValue(categoria.toString());
	            sheet.setColumnWidth(categoria.toInt(), 10000);
	       
	        }
		}
		catch (Exception ex) {
		}
	}
	
	public void geraArquivoAnaliseComparativa() {
		
		HSSFWorkbook workbook = new HSSFWorkbook();
		this.analisaComparativaWorkbook = workbook;
		
        HSSFSheet sheet = workbook.createSheet("FirstSheet"); 
        this.analiseComparativaSheet = sheet;
        
        criaHeaderAnaliseComparativa(sheet, workbook);
            
	}
	
	public void finalizaArquivoAnaliseComparativa () {
		try {
	        FileOutputStream fileOut = new FileOutputStream("teste.xls");
	        this.analisaComparativaWorkbook.write(fileOut);
	        fileOut.close();
		} catch (Exception ex) {
				
		}
	}
	
	
	public void preencheLinhaAnaliseComparativa(ArrayList<String> valoresRubrica, int linhaRubrica) {
		int contadorColuna = 0;
		HSSFRow novaLinha = this.analiseComparativaSheet.createRow((short)linhaRubrica);
		for (String valor : valoresRubrica ) {
			HSSFCell celula = novaLinha.createCell(contadorColuna);
		    celula.setCellValue(valor);
		    contadorColuna += 1;
		}			
	}
	
	
	
	
}