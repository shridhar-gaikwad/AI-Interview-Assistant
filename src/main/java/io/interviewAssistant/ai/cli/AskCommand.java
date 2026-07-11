package io.interviewAssistant.ai.cli;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import io.interviewAssistant.ai.application.OllamaService;

@ShellComponent
public class AskCommand {

    private final OllamaService ollamaService;

    public AskCommand(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @ShellMethod("Ask InterviewGPT a question")
    public String ask(String question) {
        return ollamaService.askModel(question);
    }
}
