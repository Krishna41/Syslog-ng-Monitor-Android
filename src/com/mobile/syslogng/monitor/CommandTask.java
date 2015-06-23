/*

	This program is free software: you can redistribute it and/or modify

    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
 */


package com.mobile.syslogng.monitor;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


@SuppressLint("TrulyRandom")
public class CommandTask extends AsyncTask<String, Void, String>{

	private Context context;
	private String hostName;
	private Integer portNumber;
	private String command;
	private ICommandCallBack callBack;
	
	
	
	private Boolean isException = false;
	private ProgressDialog progressDialog;
	
	public CommandTask(){
		// Empty Constructor
	}
	
	public CommandTask(ICommandCallBack callBack, Context context, String hostName, Integer portNumber, String command){
		
		this.callBack = callBack;
		this.context = context;
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.command = command;
		
	}
	
	@Override
	 protected void onPreExecute() {
	  super.onPreExecute();
	  callBack.commandExecutionStart();
	  progressDialog = new ProgressDialog(context);
	  this.progressDialog.setMessage(context.getString(R.string.command_task_progress_dialog));
	  this.progressDialog.show();
	 }
	
	@Override
	protected String doInBackground(String... params) {
		
		SSLSocket socket = null;
		SSLSession session;
		PrintWriter out = null;
		StringBuilder sBuilder = null;
		BufferedReader in = null;
		String line = null;
		
		String result = null;
		//Implementing for SELFSIGNED CERTIFICATES - Will be changed in future as per needs
		TrustManager[] trustAllCertificates = new TrustManager[] { 
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
						return new java.security.cert.X509Certificate[0]; 
					}
					@SuppressWarnings("unused")
					public void checkClientTrusted(X509Certificate[] certs, String authType) {}
					@SuppressWarnings("unused")
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {
					}
					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {
					}
				}};
		
		try
		{
			SSLContext sct = SSLContext.getInstance("SSL");
			sct.init(null, trustAllCertificates, new SecureRandom());
			
			SocketFactory socketFactory = sct.getSocketFactory();
			
			if(socket == null)
			{
				socket = (SSLSocket) socketFactory.createSocket(hostName, portNumber);
				
				session = socket.getSession();
			}
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			out.println(command);
			
			out.flush();
			in = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			sBuilder = new StringBuilder();
			
			
			while ((line  = in.readLine()) != null) 
			{
				if(!line.equals("null") && !line.equals(""))
					sBuilder.append(line);
			}
			
			result = sBuilder.toString();

		}
		catch(IOException e)
		{
			isException = true;
			Log.e("ExecuteCommand Error", e.getMessage());
			result = e.getMessage();
		} catch (KeyManagementException e) {
			isException = true;
			Log.e("ExecuteCommand Error", e.getMessage());
			result = e.getMessage();
		} catch (NoSuchAlgorithmException e) {
			isException = true;
			Log.e("ExecuteCommand Error", e.getMessage());
			result = e.getMessage();
		}
		finally
		{
			try {
				if(socket != null){
					socket.close();
				}
				if(out != null){
					out.close();
				}
				if(in != null){
					in.close();
				}
				
				session = null;
				sBuilder = null;
				
			} catch (IOException e) {
				isException = true;
				e.printStackTrace();
				result = e.getMessage();
			}
			
		}
		
		
		return result;
	}
	
	@Override
	protected void onPostExecute(String result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
			callBack.commandExecutionEnd(result, isException);
     }

}