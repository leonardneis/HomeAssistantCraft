package io.homeassistantcraft.mod.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homeassistantcraft.mod.network.ModNetwork;
import io.homeassistantcraft.mod.network.packet.SaveServiceBlockConfigPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;

public final class ServiceBlockScreen extends Screen {
    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 34;
    private static final int LABEL_OFFSET_Y = 10;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;

    private final BlockPos pos;
    private final String initialDomain;
    private final String initialService;
    private final String initialEntityId;

    private EditBox domainBox;
    private EditBox serviceBox;
    private EditBox entityIdBox;

    public ServiceBlockScreen(BlockPos pos, String domain, String service, String entityId) {
        super(new TextComponent("Service Block"));
        this.pos = pos;
        this.initialDomain = domain;
        this.initialService = service;
        this.initialEntityId = entityId;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 64;
        int yDomain = startY;
        int yService = yDomain + FIELD_SPACING;
        int yEntityId = yService + FIELD_SPACING;
        int yButtons = yEntityId + FIELD_SPACING + 6;

        domainBox = new EditBox(
            this.font,
            centerX - (FIELD_WIDTH / 2),
            yDomain,
            FIELD_WIDTH,
            FIELD_HEIGHT,
            new TextComponent("domain")
        );
        domainBox.setMaxLength(128);
        domainBox.setValue(initialDomain);

        serviceBox = new EditBox(
            this.font,
            centerX - (FIELD_WIDTH / 2),
            yService,
            FIELD_WIDTH,
            FIELD_HEIGHT,
            new TextComponent("service")
        );
        serviceBox.setMaxLength(128);
        serviceBox.setValue(initialService);

        entityIdBox = new EditBox(
            this.font,
            centerX - (FIELD_WIDTH / 2),
            yEntityId,
            FIELD_WIDTH,
            FIELD_HEIGHT,
            new TextComponent("entity_id")
        );
        entityIdBox.setMaxLength(128);
        entityIdBox.setValue(initialEntityId);

        addRenderableWidget(domainBox);
        addRenderableWidget(serviceBox);
        addRenderableWidget(entityIdBox);

        addRenderableWidget(new Button(centerX - FIELD_WIDTH / 2, yButtons, BUTTON_WIDTH, BUTTON_HEIGHT, new TextComponent("Save"), button -> {
            ModNetwork.CHANNEL.sendToServer(new SaveServiceBlockConfigPacket(
                pos,
                domainBox.getValue(),
                serviceBox.getValue(),
                entityIdBox.getValue()
            ));
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }));

        addRenderableWidget(new Button(centerX + 4, yButtons, BUTTON_WIDTH, BUTTON_HEIGHT, new TextComponent("Cancel"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }));

        setInitialFocus(domainBox);
    }

    @Override
    public void tick() {
        super.tick();
        domainBox.tick();
        serviceBox.tick();
        entityIdBox.tick();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (domainBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (serviceBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (entityIdBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (domainBox.keyPressed(keyCode, scanCode, modifiers) || domainBox.canConsumeInput()) {
            return true;
        }
        if (serviceBox.keyPressed(keyCode, scanCode, modifiers) || serviceBox.canConsumeInput()) {
            return true;
        }
        if (entityIdBox.keyPressed(keyCode, scanCode, modifiers) || entityIdBox.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 64;
        int yDomain = startY;
        int yService = yDomain + FIELD_SPACING;
        int yEntityId = yService + FIELD_SPACING;

        drawCenteredString(poseStack, this.font, this.title, centerX, startY - 18, 0xFFFFFF);
        drawString(
            poseStack,
            this.font,
            new TextComponent("domain"),
            centerX - (FIELD_WIDTH / 2),
            yDomain - LABEL_OFFSET_Y,
            0xA0A0A0
        );
        drawString(
            poseStack,
            this.font,
            new TextComponent("service"),
            centerX - (FIELD_WIDTH / 2),
            yService - LABEL_OFFSET_Y,
            0xA0A0A0
        );
        drawString(
            poseStack,
            this.font,
            new TextComponent("entity_id"),
            centerX - (FIELD_WIDTH / 2),
            yEntityId - LABEL_OFFSET_Y,
            0xA0A0A0
        );

        domainBox.render(poseStack, mouseX, mouseY, partialTick);
        serviceBox.render(poseStack, mouseX, mouseY, partialTick);
        entityIdBox.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
