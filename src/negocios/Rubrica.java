package negocios;
import java.util.*;

/**
 *  Classe que representa uma Rubrica de um Plano de Contas
 *
 */
public class Rubrica {
	
	private Rubrica pai;
	private String nome;
	private int codigo;
	private CategoriaRubrica categoria;
	private Double[] valoresAnoPassado;
	private Double[] valoresPrevistos;
	private Double[] valoresRealizados;
	private List<Rubrica> subRubricas;
	
	public Rubrica(Rubrica pai, String nome, int codigo, CategoriaRubrica categoria, Double[] valoresAnoPassado) {
		this.pai = pai;
		this.nome = nome;
		this.codigo = codigo;
		this.categoria = categoria;
		this.subRubricas = new ArrayList<>();
		this.valoresAnoPassado = valoresAnoPassado; 
		this.valoresPrevistos = new Double[12]; 
		this.valoresRealizados = new Double[12]; 
	}


	public Rubrica getPai() {
		return this.pai;
	}
	public int getCodigo() {
		return codigo;
	}
	public List<Rubrica> getSubRubricas(){
		return this.subRubricas;
	
	}
	
	public void addSubRubrica(Rubrica subRubrica) {
		this.subRubricas.add(subRubrica);
	}
	
	public void setValorRealizado(int mes, double valor) {
		this.valoresRealizados[mes] = valor;
	}
	
	public void setValorPrevisto(int mes, double valor) {
		this.valoresPrevistos[mes] = valor;
	}
	
	public String getValorPrevisto(int mes, int codigo) {
		String out = this.nome + "  " +  String.valueOf(this.codigo) +" "+this.valoresPrevistos[mes];
		return out;
	}
	public Double getValorPrevisto(int mes) {
		return this.valoresPrevistos[mes];
	}
	
	
	public double getvalorAnoPassado(int mes) {
		return this.valoresAnoPassado[mes];
	}
	
	public String getNome() {
		return this.nome;
	}
	
	public CategoriaRubrica getCategoria () {
		return this.categoria;
	}
	public Double getValorRealizado(int mes) {
		return this.valoresRealizados[mes];
	}

	


	public String toString() {
		String out = this.nome + "  " +  String.valueOf(this.codigo);
		double total = 0;
		for(int i = 0; i < 12; i++) {
			total += this.valoresAnoPassado[i];
		}
		out += "  " + Double.toString(total);
		return out + '\n';
	}
}
