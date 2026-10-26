package io.github.zensu357.camswap;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import io.github.zensu357.camswap.utils.VideoManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CameraSourceSwitchTest {
    @Test
    public void configBroadcastSwitchesBothWaysWithoutRestartingMedia() throws Exception {
        try (MockedStatic<Log> ignored = Mockito.mockStatic(Log.class);
             MockedStatic<VideoManager> videos = Mockito.mockStatic(VideoManager.class)) {
            ConfigManager config = new ConfigManager(false);
            videos.when(VideoManager::getConfig).thenReturn(config);
            ConfigWatcher.Callback callback = mock(ConfigWatcher.Callback.class);
            ConfigWatcher watcher = new ConfigWatcher(callback);
            Method update = ConfigWatcher.class.getDeclaredMethod("handleConfigUpdate", Intent.class);
            update.setAccessible(true);
            Intent intent = mock(Intent.class);
            when(intent.getStringExtra(IpcContract.EXTRA_CONFIG_JSON))
                    .thenReturn("{\"use_original_camera\":true}",
                            "{\"use_original_camera\":true}",
                            "{\"use_original_camera\":false}");

            assertFalse(SwitchablePreview.useOriginalCamera());
            update.invoke(watcher, intent);
            assertTrue(SwitchablePreview.useOriginalCamera());
            update.invoke(watcher, intent);
            update.invoke(watcher, intent);
            assertFalse(SwitchablePreview.useOriginalCamera());
            verify(callback, times(2)).onCameraSourceChanged();
            verify(callback, never()).onMediaSourceChanged();
            verify(callback, never()).onRotationChanged(anyInt());
        }
    }

    @Test
    public void sessionAndRequestReuseCameraTargetIncludingNativeAliases() throws Exception {
        Surface output = mock(Surface.class);
        Surface alias = mock(Surface.class);
        Surface cameraTarget = mock(Surface.class);
        Surface mediaTarget = mock(Surface.class);
        when(cameraTarget.isValid()).thenReturn(true);
        when(output.toString()).thenReturn("Surface(mNativeObject=123)");
        when(alias.toString()).thenReturn("Surface(mNativeObject=123)");
        SwitchablePreview preview = mock(SwitchablePreview.class);
        when(preview.getCameraSurface()).thenReturn(cameraTarget);
        when(preview.getMediaSurface()).thenReturn(mediaTarget);
        try (MockedStatic<SystemClock> clock = Mockito.mockStatic(SystemClock.class);
             MockedStatic<SwitchablePreview> factory = Mockito.mockStatic(SwitchablePreview.class)) {
            Camera2SessionHook hook = new Camera2SessionHook(new MediaPlayerManager());
            factory.when(() -> SwitchablePreview.create(output)).thenReturn(preview);
            assertSame(cameraTarget, hook.getCameraTargetSurface(output));
            assertSame(cameraTarget, hook.getCameraTargetSurface(output));
            assertSame(cameraTarget, hook.getCameraTargetSurface(alias));
            assertSame(cameraTarget, hook.getCameraTargetSurface(cameraTarget));
            Method mediaOutput = Camera2SessionHook.class.getDeclaredMethod("mediaOutputSurface", Surface.class);
            mediaOutput.setAccessible(true);
            assertSame(mediaTarget, mediaOutput.invoke(hook, alias));
            factory.verify(() -> SwitchablePreview.create(output), times(1));
        }
    }

    @Test
    public void previewSetupFailureKeepsCameraOffTheRealOutput() {
        Surface output = mock(Surface.class);
        try (MockedStatic<Log> log = Mockito.mockStatic(Log.class);
             MockedStatic<SystemClock> clock = Mockito.mockStatic(SystemClock.class);
             MockedStatic<SwitchablePreview> factory = Mockito.mockStatic(SwitchablePreview.class);
             MockedConstruction<HandlerThread> threads = Mockito.mockConstruction(HandlerThread.class);
             MockedConstruction<Handler> handlers = Mockito.mockConstruction(Handler.class);
             MockedConstruction<SurfaceTexture> textures = Mockito.mockConstruction(SurfaceTexture.class);
             MockedConstruction<Surface> surfaces = Mockito.mockConstruction(Surface.class,
                     (surface, context) -> when(surface.isValid()).thenReturn(true))) {
            Camera2SessionHook hook = new Camera2SessionHook(new MediaPlayerManager());
            factory.when(() -> SwitchablePreview.create(output)).thenReturn(null);
            Surface target = hook.getCameraTargetSurface(output);
            assertNotSame(output, target);
            assertTrue(hook.isVirtualSurface(target));
            assertSame(target, hook.getCameraTargetSurface(output));
        }
    }
}
