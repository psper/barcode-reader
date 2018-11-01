/*
 * Copyright (C) 2013 Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.easy.barcodereader;
import java.io.Serializable;

public class Contact implements Serializable {
	
	private String userCode;
	private String name;
	private String companyNumber;
	private String senhas;
	private String ipod;
	private String mesa;
	private String state;	
	
	

	public Contact(String userCode, String name, String companyNumber, String senhas, String ipod, String mesa, String state) {
		super();
		this.userCode = userCode;
		this.name = name;
		this.companyNumber = companyNumber;
		this.senhas = senhas;
		this.ipod = ipod;
		this.mesa = mesa;
		this.state = state;
	}
	public String getUserCode() {
		return userCode;
	}
	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}
	public void setCompanyNumber(String companyNumber) {
		this.companyNumber = companyNumber;
	}
	
	public String getSenhas() {
		return senhas;
	}
	public void setSenhas(String senhas) {
		this.senhas = senhas;
	}
	
	public String getIpod() {
		return ipod;
	}
	public void setIpod(String ipod) {
		this.ipod = ipod;
	}
	
	public String getMesa() {
		return mesa;
	}
	public void setMesa(String mesa) {
		this.mesa = mesa;
	}
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
}
