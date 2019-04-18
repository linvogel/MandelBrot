import java.math.BigDecimal;

public class BigComplexNumber {
	
	BigDecimal real;
	BigDecimal imaginary;
	
	public BigComplexNumber() {
		real = BigDecimal.ZERO;
		imaginary = BigDecimal.ZERO;
	}
	
	public BigComplexNumber(BigDecimal real, BigDecimal imaginary) {
		this.real = real;
		this.imaginary = imaginary;
	}

	public BigComplexNumber multiply(BigComplexNumber number) {
		return new BigComplexNumber(real.multiply(number.real, MandelBrot.mc).subtract(imaginary.multiply(number.imaginary, MandelBrot.mc), MandelBrot.mc),
				real.multiply(number.imaginary, MandelBrot.mc).add(imaginary.multiply(number.real, MandelBrot.mc), MandelBrot.mc));
	}

	public BigComplexNumber add(BigComplexNumber number) {
		return new BigComplexNumber(real.add(number.real, MandelBrot.mc), imaginary.add(number.imaginary, MandelBrot.mc));
	}

	public BigDecimal absSq() {
		return real.multiply(real, MandelBrot.mc).add(imaginary.multiply(imaginary, MandelBrot.mc), MandelBrot.mc);
	}
	
	@Override
	public String toString() {
		return "(" + real + " + " + imaginary + "i)";
	}
}
