package jp.co.sss.pr.form;

import java.util.List;

import jakarta.validation.Valid;

public class AnswerListForm {
	
	@Valid
	private List<AnswerForm> results;

	public List<AnswerForm> getResults() {
		return results;
	}

	public void setResults(List<AnswerForm> results) {
		this.results = results;
	}
	
	

}
