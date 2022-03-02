package com.github.kchobantonov.camunda.jsonforms.invoice;

import java.util.logging.Logger;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.value.FileValue;

public class ArchiveInvoiceService implements JavaDelegate {

  private static final Logger LOGGER = Logger.getLogger(ArchiveInvoiceService.class.getName());

  public void execute(DelegateExecution execution) throws Exception {

    Boolean shouldFail = (Boolean) execution.getVariable("shouldFail");
    FileValue invoiceDocumentVar  = execution.getVariableTyped("invoiceDocument");

    if(shouldFail != null && shouldFail) {
      throw new ProcessEngineException("Could not archive invoice...");
    }
    else {
      LOGGER.info("\n\n  ... Now archiving invoice "+execution.getVariable("invoiceNumber")
          +", filename: "+invoiceDocumentVar.getFilename()+" \n\n");
    }

  }

}
