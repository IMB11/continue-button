package com.mineblock11.continuebutton.mixin;

import com.mineblock11.continuebutton.ContinueButtonMod;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.QuickPlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = TitleScreen.class, priority = 1001)
public class MixinTitleScreen extends Screen {
    private final MultiplayerServerListPinger serverListPinger = new MultiplayerServerListPinger();
    ButtonWidget continueButtonWidget = null;
    private ServerInfo serverInfo = null;
    private boolean isFirstRender = false;
    private boolean readyToShow = false;

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(at = @At("HEAD"), method = "initWidgetsNormal(II)V")
    public void drawMenuButton(int y, int spacingY, CallbackInfo info) {
        ButtonWidget.Builder continueButtonBuilder = ButtonWidget.builder(Text.translatable("continuebutton.continueButtonTitle"), button -> {
            if (ContinueButtonMod.lastLocal) {
                if (!ContinueButtonMod.serverName.isBlank()) {
                    QuickPlay.startSingleplayer(client, ContinueButtonMod.serverAddress);
                } else {
                    CreateWorldScreen.create(this.client, this);
                }
            } else {
                QuickPlay.startMultiplayer(client, ContinueButtonMod.serverAddress);
            }
        });
        continueButtonBuilder.dimensions(this.width / 2 - 100, y, 98, 20);
        continueButtonWidget = continueButtonBuilder.build();
        Screens.getButtons(this).add(continueButtonWidget);

    }

    @Inject(at = @At("HEAD"), method = "init()V")
    public void initAtHead(CallbackInfo info) {
        this.isFirstRender = true;
    }

    @Inject(at = @At("TAIL"), method = "init()V")
    public void init(CallbackInfo info) {
        for (ClickableWidget button : Screens.getButtons(this)) {
            if (button.visible && !button.getMessage().equals(Text.translatable("continuebutton.continueButtonTitle"))) {
                button.setX(this.width / 2 + 2);
                button.setWidth(98);
                break;
            }
        }
    }

    private void atFirstRender() {
        new Thread(() -> {
            if (!ContinueButtonMod.lastLocal) {
                serverInfo = new ServerInfo(ContinueButtonMod.serverName, ContinueButtonMod.serverAddress, ServerInfo.ServerType.OTHER);
                serverInfo.label = Text.translatable("multiplayer.status.pinging");
                try {
                    Runnable donothing = () -> {};
                    serverListPinger.add(serverInfo, donothing, donothing);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            readyToShow = true;
        }).start();
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void renderAtHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (isFirstRender) {
            isFirstRender = false;
            atFirstRender();
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void renderAtTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (continueButtonWidget.isHovered() && this.readyToShow) {
            if (ContinueButtonMod.lastLocal) {
                if (ContinueButtonMod.serverAddress.isEmpty()) {
                    List<OrderedText> list = new ArrayList<>();
                    list.add(Text.translatable("selectWorld.create").formatted(Formatting.GRAY).asOrderedText());
                    context.drawOrderedTooltip(this.textRenderer, list, mouseX, mouseY);
                } else {
                    List<OrderedText> list = new ArrayList<>();
                    list.add(Text.translatable("menu.singleplayer").formatted(Formatting.GRAY).asOrderedText());
                    list.add(Text.literal(ContinueButtonMod.serverName).asOrderedText());
                    context.drawOrderedTooltip(this.textRenderer, list, mouseX, mouseY);
                }
            } else {
                List<OrderedText> list = new ArrayList<>(this.client.textRenderer.wrapLines(serverInfo.label, 270));
                list.add(0, Text.literal(serverInfo.name).formatted(Formatting.GRAY).asOrderedText());
                context.drawOrderedTooltip(this.textRenderer, list, mouseX, mouseY);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "tick()V")
    public void tick(CallbackInfo info) {
        serverListPinger.tick();
    }

    @Inject(at = @At("RETURN"), method = "removed()V")
    public void removed(CallbackInfo info) {
        serverListPinger.cancel();
    }
}