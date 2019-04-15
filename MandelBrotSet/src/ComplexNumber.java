
public class ComplexNumber {
	
	double real;
	double imaginary;
	
	public ComplexNumber(double real, double imaginary) {
		this.real = real;
		this.imaginary = imaginary;
	}
	
	public ComplexNumber() {
		this.real = this.imaginary = 0;
	}
	
	public ComplexNumber multiply(ComplexNumber number) {
		return new ComplexNumber(this.real*number.real - this.imaginary*number.imaginary, this.real*number.imaginary + this.imaginary*number.real);
	}
	
	public ComplexNumber add(ComplexNumber number) {
		return new ComplexNumber(this.real + number.real, this.imaginary + number.imaginary);
	}
	
	public double absSq() {
		return this.real*this.real + this.imaginary*this.imaginary;
	}
	
}
