/* ******************************************************************************
 * This file is part of Optsicom Remote Experiment System client
 * 
 * License:
 *   EPL: http://www.eclipse.org/legal/epl-v10.html
 *   See the LICENSE file in the project's top-level directory for details.
 *   
 * Contributors:
 *   Optsicom(http://www.optsicom.es), Sidelab (http://www.sidelab.es) 
 *   and others
 * **************************************************************************** */
package es.optsicom.res.server;


import java.io.Serializable;

/**
 * Clase para representar el formato que tendra el paquete de datos
 * que sera intercambiado entre cliente y servidor
 * y contendra los bytes de los ficheros
 * @author Carlos Llorente Lopez
 *
 */
public class PaqueteDatos implements Serializable {
	
	private static final long serialVersionUID = 2030175987424385394L;
	private byte[] buf;
	private int longitud;
	
	/**
	 * Constructor de la clase
	 * @param buf Array con parte de los bytes del fichero
	 * @param longitud Longitud de los bytes que viajan en el array.
	 */
	public PaqueteDatos(byte[] buf,int longitud){
		this.buf = buf.clone();
		this.longitud = longitud;
	}

	/**
	 * Metodo para establecer el array de bytes del paquete de datos
	 * @param buf Array de bytes del fichero
	 */
	public void setBuf(byte[] buf) {
		this.buf = buf;
	}

	/**
	 * Metodo para conseguir el array de bytes del paquete de datos
	 * @return buf Devuelve un array con los bytes del paquete
	 */
	public byte[] getBuf() {
		return buf;
	}

	/**
	 * Metodo para asignar la longitud de los datos al paquete de datos
	 * @param longitud Entero con la longitud de los datos del paquete
	 */
	public void setLongitud(int longitud) {
		this.longitud = longitud;
	}

	/**
	 * Metodo para conseguir la longitud de los datos del paquete
	 * @return Devuelve la longitud de los datos del paquete
	 */
	public int getLongitud() {
		return longitud;
	}

}
