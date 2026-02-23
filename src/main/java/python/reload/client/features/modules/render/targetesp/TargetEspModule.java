package python.reload.client.features.modules.render.targetesp;

import lombok.Getter;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.BooleanSetting;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.module.setting.SliderSetting;


import python.reload.client.features.modules.render.targetesp.modes.TargetEspComets;
import python.reload.client.features.modules.render.targetesp.modes.TargetEspCrystal;
import python.reload.client.features.modules.render.targetesp.modes.TargetEspCircle;
import python.reload.client.features.modules.render.targetesp.modes.TargetEspTexture;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();
    private final TargetEspCircle espCircle = new TargetEspCircle();
    private final TargetEspComets espComets = new TargetEspComets();
    private final TargetEspTexture espTexture = new TargetEspTexture();
    private final TargetEspCrystal espCrystal = new TargetEspCrystal();

    private TargetEspMode currentMode = espTexture; // порно

    @Getter public final ModeSetting mode = new ModeSetting("Mode").value("Marker").values("Marker", "Comets", "Ghost", "Circle", "Chain", "Crystal").onAction(() -> {
        currentMode = switch (getMode().getValue()) {
            case "Comets" -> espComets;
            case "Crystal" -> espCrystal;
            case "Circle" -> espCircle;
            default -> espTexture;
        };
    });

    public final ModeSetting animation = new ModeSetting("Animation").value("In").values("In", "Out", "None");
    public final SliderSetting duration = new SliderSetting("Duration").value(3f).range(1f, 20f).step(1f);
    public final SliderSetting size = new SliderSetting("Size").value(1f).range(0.1f, 2f).step(0.1f);
    public final SliderSetting inSize = new SliderSetting("In size").value(0f).range(0f, 1f).step(0.1f).setVisible(() -> animation.is("In"));
    public final SliderSetting outSize = new SliderSetting("Out size").value(2f).range(1f, 2f).step(0.1f).setVisible(() -> animation.is("Out"));
    public final BooleanSetting lastPosition = new BooleanSetting("Last position").value(true);


    public final ModeSetting markerTexture = new ModeSetting("Marker Texture").value("1").values("1", "2", "3", "4").setVisible(() -> mode.is("Marker"));
    public final BooleanSetting markerRedOnImpact = new BooleanSetting("Marker Red on Impact").value(true).setVisible(() -> mode.is("Marker"));
    public final SliderSetting markerImpactFadeIn = new SliderSetting("Marker Impact Fade In").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Marker") && markerRedOnImpact.getValue());
    public final SliderSetting markerImpactFadeOut = new SliderSetting("Marker Impact Fade Out").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Marker") && markerRedOnImpact.getValue());
    public final SliderSetting markerImpactIntensity = new SliderSetting("Marker Impact Intensity").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Marker") && markerRedOnImpact.getValue());


    public final SliderSetting cometSize = new SliderSetting("Comet Size").value(0.25f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Comets"));
    public final SliderSetting cometWidth = new SliderSetting("Comet Width").value(1f).range(0.5f, 2f).step(0.05f).setVisible(() -> mode.is("Comets"));
    public final SliderSetting cometHeight = new SliderSetting("Comet Height").value(1f).range(0.5f, 2f).step(0.05f).setVisible(() -> mode.is("Comets"));

    public final SliderSetting trailLength = new SliderSetting("Trail Length").value(21f).range(3f, 80f).step(1f).setVisible(() -> mode.is("Comets"));
    public final SliderSetting trailMinSize = new SliderSetting("Trail Min Size").value(0.4f).range(0.05f, 1f).step(0.05f).setVisible(() -> mode.is("Comets"));
    public final SliderSetting trailFadeStart = new SliderSetting("Trail Fade Start").value(0f).range(0f, 1f).step(0.05f).setVisible(() -> mode.is("Comets"));

    public final SliderSetting orbitRadius = new SliderSetting("Orbit Radius").value(1.15f).range(0.3f, 4f).step(0.05f).setVisible(() -> mode.is("Comets"));
    public final SliderSetting verticalAmplitude = new SliderSetting("Vertical Amplitude").value(0.4f).range(0f, 2f).step(0.05f).setVisible(() -> mode.is("Comets"));
    public final SliderSetting orbitOffset = new SliderSetting("Orbit Y Offset").value(0f).range(-1f, 1f).step(0.05f).setVisible(() -> mode.is("Comets"));

    public final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed").value(15f).range(1f, 50f).step(0.5f).setVisible(() -> mode.is("Comets"));
    public final BooleanSetting reverseRotation = new BooleanSetting("Reverse Rotation").value(false).setVisible(() -> mode.is("Comets"));
    public final SliderSetting verticalSpeed = new SliderSetting("Vertical Speed").value(1f).range(0.1f, 3f).step(0.1f).setVisible(() -> mode.is("Comets"));

    public final SliderSetting cometCount = new SliderSetting("Comet Count").value(3f).range(1f, 10f).step(1f).setVisible(() -> mode.is("Comets"));
    public final BooleanSetting symmetricBounce = new BooleanSetting("Symmetric Bounce").value(true).setVisible(() -> mode.is("Comets"));

    public final BooleanSetting redOnImpact = new BooleanSetting("Red on Impact").value(true).setVisible(() -> mode.is("Comets"));
    public final SliderSetting impactFadeIn = new SliderSetting("Impact Fade In").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Comets") && redOnImpact.getValue());
    public final SliderSetting impactFadeOut = new SliderSetting("Impact Fade Out").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Comets") && redOnImpact.getValue());
    public final SliderSetting impactIntensity = new SliderSetting("Impact Intensity").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Comets") && redOnImpact.getValue());

    public final BooleanSetting pulseEffect = new BooleanSetting("Pulse Effect").value(false).setVisible(() -> mode.is("Comets"));
    public final SliderSetting pulseSpeed = new SliderSetting("Pulse Speed").value(5f).range(1f, 20f).step(0.5f).setVisible(() -> mode.is("Comets") && pulseEffect.getValue());
    public final SliderSetting pulseIntensity = new SliderSetting("Pulse Intensity").value(0.2f).range(0.05f, 0.5f).step(0.01f).setVisible(() -> mode.is("Comets") && pulseEffect.getValue());


    public final SliderSetting ghostSize = new SliderSetting("Ghost Size").value(1f).range(0.1f, 3f).step(0.1f).setVisible(() -> mode.is("Ghost"));
    public final SliderSetting ghostCount = new SliderSetting("Ghost Count").value(3f).range(1f, 5f).step(1f).setVisible(() -> mode.is("Ghost"));
    public final SliderSetting ghostOrbitRadius = new SliderSetting("Ghost Orbit Radius").value(1f).range(0.3f, 3f).step(0.1f).setVisible(() -> mode.is("Ghost"));
    public final SliderSetting ghostVerticalAmplitude = new SliderSetting("Ghost Vertical Amplitude").value(1f).range(0.3f, 3f).step(0.1f).setVisible(() -> mode.is("Ghost"));
    public final SliderSetting ghostTrailLength = new SliderSetting("Ghost Trail Length").value(20f).range(5f, 40f).step(1f).setVisible(() -> mode.is("Ghost"));
    public final BooleanSetting ghostRedOnImpact = new BooleanSetting("Ghost Red on Impact").value(true).setVisible(() -> mode.is("Ghost"));
    public final SliderSetting ghostImpactFadeIn = new SliderSetting("Ghost Impact Fade In").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Ghost") && ghostRedOnImpact.getValue());
    public final SliderSetting ghostImpactFadeOut = new SliderSetting("Ghost Impact Fade Out").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Ghost") && ghostRedOnImpact.getValue());
    public final SliderSetting ghostImpactIntensity = new SliderSetting("Ghost Impact Intensity").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Ghost") && ghostRedOnImpact.getValue());


    public final SliderSetting circleSize = new SliderSetting("Circle Size").value(1f).range(0.3f, 3f).step(0.1f).setVisible(() -> mode.is("Circle"));
    public final SliderSetting circleSpeed = new SliderSetting("Circle Speed").value(3f).range(0.5f, 10f).step(0.5f).setVisible(() -> mode.is("Circle"));
    public final BooleanSetting circleBloom = new BooleanSetting("Circle Bloom").value(true).setVisible(() -> mode.is("Circle"));
    public final SliderSetting circleBloomSize = new SliderSetting("Circle Bloom Size").value(0.3f).range(0.1f, 0.8f).step(0.1f).setVisible(() -> mode.is("Circle") && circleBloom.getValue());
    public final BooleanSetting circleRedOnImpact = new BooleanSetting("Circle Red on Impact").value(true).setVisible(() -> mode.is("Circle"));
    public final SliderSetting circleImpactFadeIn = new SliderSetting("Circle Impact Fade In").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Circle") && circleRedOnImpact.getValue());
    public final SliderSetting circleImpactFadeOut = new SliderSetting("Circle Impact Fade Out").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Circle") && circleRedOnImpact.getValue());
    public final SliderSetting circleImpactIntensity = new SliderSetting("Circle Impact Intensity").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Circle") && circleRedOnImpact.getValue());


    public final SliderSetting chainWidth = new SliderSetting("Chain Width").value(1f).range(0.5f, 2f).step(0.1f).setVisible(() -> mode.is("Chain"));
    public final SliderSetting chainSpeed = new SliderSetting("Chain Speed").value(2f).range(0.5f, 10f).step(0.5f).setVisible(() -> mode.is("Chain"));
    public final BooleanSetting chainRedOnImpact = new BooleanSetting("Chain Red on Impact").value(true).setVisible(() -> mode.is("Chain"));
    public final SliderSetting chainImpactFadeIn = new SliderSetting("Chain Impact Fade In").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Chain") && chainRedOnImpact.getValue());
    public final SliderSetting chainImpactFadeOut = new SliderSetting("Chain Impact Fade Out").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Chain") && chainRedOnImpact.getValue());
    public final SliderSetting chainImpactIntensity = new SliderSetting("Chain Impact Intensity").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Chain") && chainRedOnImpact.getValue());

    public final SliderSetting crystalSpeed = new SliderSetting("Crystal Speed").value(6f).range(1f, 20f).step(0.5f).setVisible(() -> mode.is("Crystal"));
    public final BooleanSetting crystalBloom = new BooleanSetting("Crystal Bloom").value(true).setVisible(() -> mode.is("Crystal"));
    public final SliderSetting crystalBloomSize = new SliderSetting("Crystal Bloom Size").value(1f).range(0.3f, 2f).step(0.1f).setVisible(() -> mode.is("Crystal") && crystalBloom.getValue());
    public final BooleanSetting crystalRedOnImpact = new BooleanSetting("Crystal Red on Impact").value(true).setVisible(() -> mode.is("Crystal"));
    public final SliderSetting crystalImpactFadeIn = new SliderSetting("Crystal Impact Fade In").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Crystal") && crystalRedOnImpact.getValue());
    public final SliderSetting crystalImpactFadeOut = new SliderSetting("Crystal Impact Fade Out").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Crystal") && crystalRedOnImpact.getValue());
    public final SliderSetting crystalImpactIntensity = new SliderSetting("Crystal Impact Intensity").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Crystal") && crystalRedOnImpact.getValue());

    public TargetEspModule() {
        addSettings(
                mode, animation, duration, size, inSize, outSize, lastPosition,

                markerTexture, markerRedOnImpact, markerImpactFadeIn, markerImpactFadeOut, markerImpactIntensity,

                cometSize, cometWidth, cometHeight,
                trailLength, trailMinSize, trailFadeStart,
                orbitRadius, verticalAmplitude, orbitOffset,
                rotationSpeed, reverseRotation, verticalSpeed,
                cometCount, symmetricBounce,
                redOnImpact, impactFadeIn, impactFadeOut, impactIntensity,
                pulseEffect, pulseSpeed, pulseIntensity,

                ghostSize, ghostCount, ghostOrbitRadius, ghostVerticalAmplitude, ghostTrailLength,
                ghostRedOnImpact, ghostImpactFadeIn, ghostImpactFadeOut, ghostImpactIntensity,

                circleSize, circleSpeed, circleBloom, circleBloomSize,
                circleRedOnImpact, circleImpactFadeIn, circleImpactFadeOut, circleImpactIntensity,

                chainWidth, chainSpeed, chainRedOnImpact, chainImpactFadeIn, chainImpactFadeOut, chainImpactIntensity,

                crystalSpeed, crystalBloom, crystalBloomSize,
                crystalRedOnImpact, crystalImpactFadeIn, crystalImpactFadeOut, crystalImpactIntensity
        );
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            TargetEspMode.updatePositions();
            currentMode.onRender3D(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.updateAnimation(duration.getValue().longValue() * 50, animation.getValue(), size.getValue(), inSize.getValue(), outSize.getValue());
            currentMode.updateTarget();
            currentMode.onUpdate();
        }));

        addEvents(render3DEvent, updateEvent);
    }
}