package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TriviaScreen extends Screen {
    private final Screen parent;
    private final Runnable onSuccess;
    private final Runnable onCancel;
    private final boolean isSkid;

    public static boolean skidUnlocked = false;

    private int score = 0;
    private long errorTime = 0;
    private boolean isError = false;

    private static class Question {
        String text;
        String[] options;
        int correctAnswer;

        Question(String text, String[] options, int correctAnswer) {
            this.text = text;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }
    }

    private final List<Question> ALL_QUESTIONS = List.of(
        new Question(
            "What is the rarest block in Minecraft?",
            new String[]{"A. Dragon Egg", "B. Deepslate Emerald Ore", "C. Infested Cobblestone"},
            2 // C
        ),
        new Question(
            "What is the rarest mob in Minecraft?",
            new String[]{"A. Pink Sheep", "B. Brown Mooshroom", "C. Baby zombie villager on a chicken with full enchanted diamond armor"},
            2 // C
        ),
        new Question(
            "What's the rarest item in Minecraft?",
            new String[]{"A. Bubble Column item", "B. Leather Helmet with efficiency 1", "C. Vex Armor trim"},
            0 // A
        ),
        new Question(
            "Which mob drops the Totem of Undying?",
            new String[]{"A. Evoker", "B. Vindicator", "C. Pillager"},
            0 // A
        )
    );

    private List<Question> selectedQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;

    public TriviaScreen(Screen parent, Runnable onSuccess, Runnable onCancel, boolean isSkid) {
        super(Text.of("Trivia Quiz"));
        this.parent = parent;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
        this.isSkid = isSkid;
        pickNewQuestions();
    }

    private void pickNewQuestions() {
        this.selectedQuestions = new ArrayList<>(ALL_QUESTIONS);
        Collections.shuffle(this.selectedQuestions);
        // We only need 2
        this.selectedQuestions = this.selectedQuestions.subList(0, 2);
        this.currentQuestionIndex = 0;
        this.score = 0;
    }

    @Override
    protected void init() {
        super.init();
        buildGui();
    }

    private void buildGui() {
        this.clearChildren();

        Question q = selectedQuestions.get(currentQuestionIndex);

        int maxTextWidth = 0;
        if (this.textRenderer != null) {
            for (String opt : q.options) {
                maxTextWidth = Math.max(maxTextWidth, this.textRenderer.getWidth(opt));
            }
        }
        int buttonWidth = Math.max(200, maxTextWidth + 20);
        int buttonHeight = 20;
        int spacing = 24;

        int startY = this.height / 2 - 20;

        for (int i = 0; i < q.options.length; i++) {
            final int answerIndex = i;
            this.addDrawableChild(ButtonWidget.builder(Text.of(q.options[i]), button -> {
                if (isError) return; // Prevent clicking while error is showing

                if (answerIndex == q.correctAnswer) {
                    score++;
                    if (score >= 2) {
                        if (isSkid) {
                            skidUnlocked = true;
                        }
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } else {
                        currentQuestionIndex++;
                        this.init(); // Rebuild GUI for next question
                    }
                } else {
                    // Wrong answer
                    isError = true;
                    errorTime = System.currentTimeMillis();
                }
            }).dimensions(this.width / 2 - buttonWidth / 2, startY + (i * spacing), buttonWidth, buttonHeight).build());
        }

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            if (onCancel != null) {
                onCancel.run();
            } else {
                this.close();
            }
        }).dimensions(this.width / 2 - buttonWidth / 2, startY + (3 * spacing) + 12, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (isError) {
            if (System.currentTimeMillis() - errorTime > 1000) {
                isError = false;
                pickNewQuestions();
                if (this.client != null) {
                    this.init(); // Rebuild for new questions
                }
            }
            context.drawCenteredTextWithShadow(this.textRenderer, "Incorrect", this.width / 2, 20, 0xFFFF5555);
        } else {
            Question q = selectedQuestions.get(currentQuestionIndex);
            context.drawCenteredTextWithShadow(this.textRenderer, q.text, this.width / 2, 40, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "Question " + (currentQuestionIndex + 1) + " of 2", this.width / 2, 20, 0xFFAAAAAA);
        }
    }

    @Override
    public void close() {
        if (onCancel != null) {
            onCancel.run();
        } else if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
