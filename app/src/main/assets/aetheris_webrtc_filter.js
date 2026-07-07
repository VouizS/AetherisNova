(function () {
  try {
    if (window.__AETHERIS_WEBRTC_FILTER_INSTALLED__) {
      return;
    }

    window.__AETHERIS_WEBRTC_FILTER_INSTALLED__ = true;

    function getMode() {
      try {
        if (window.AetherisBridge && window.AetherisBridge.getVideoFilterMode) {
          return String(window.AetherisBridge.getVideoFilterMode() || "off");
        }
      } catch (e) {}
      return "off";
    }

    function isFilterEnabled() {
      var mode = getMode();
      return mode && mode !== "off";
    }

    function cssFilterForMode(mode) {
      if (mode === "gray") return "grayscale(1) contrast(1.08)";
      if (mode === "cool") return "saturate(1.18) hue-rotate(180deg) brightness(1.04)";
      if (mode === "contrast") return "contrast(1.28) saturate(1.08) brightness(1.03)";
      if (mode === "orbit") return "contrast(1.1) saturate(1.22) brightness(1.05)";
      return "none";
    }

    function drawOrbitOverlay(ctx, w, h) {
      try {
        var t = Date.now() / 1000;
        ctx.save();
        ctx.globalAlpha = 0.18;
        ctx.strokeStyle = "rgba(68,232,255,0.65)";
        ctx.lineWidth = Math.max(2, Math.floor(w / 260));
        ctx.beginPath();
        ctx.ellipse(w * 0.5, h * 0.52, w * 0.34, h * 0.22, Math.sin(t) * 0.08, 0, Math.PI * 2);
        ctx.stroke();
        ctx.globalAlpha = 0.10;
        ctx.fillStyle = "rgba(108,125,255,0.55)";
        ctx.beginPath();
        ctx.arc(w * 0.78, h * 0.20, Math.max(24, w * 0.06), 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      } catch (e) {}
    }

    async function applyAetherisFilterToStream(inputStream) {
      try {
        var mode = getMode();
        if (!mode || mode === "off") return inputStream;

        var videoTracks = inputStream.getVideoTracks ? inputStream.getVideoTracks() : [];
        if (!videoTracks || !videoTracks.length) return inputStream;

        var originalVideoTrack = videoTracks[0];
        var settings = originalVideoTrack.getSettings ? originalVideoTrack.getSettings() : {};
        var width = settings.width || 640;
        var height = settings.height || 480;
        var fps = settings.frameRate || 24;
        if (!fps || fps < 1 || fps > 60) fps = 24;

        var hiddenVideo = document.createElement("video");
        hiddenVideo.muted = true;
        hiddenVideo.autoplay = true;
        hiddenVideo.playsInline = true;
        hiddenVideo.setAttribute("playsinline", "true");
        hiddenVideo.srcObject = new MediaStream([originalVideoTrack]);
        hiddenVideo.style.cssText = "position:fixed;left:-9999px;top:-9999px;width:1px;height:1px;opacity:0;pointer-events:none;";

        var parent = document.documentElement || document.body;
        if (parent) parent.appendChild(hiddenVideo);

        try { await hiddenVideo.play(); } catch (e) {}

        var canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        canvas.style.cssText = "position:fixed;left:-9999px;top:-9999px;width:1px;height:1px;opacity:0;pointer-events:none;";
        if (parent) parent.appendChild(canvas);

        var ctx = canvas.getContext("2d", { willReadFrequently: true });
        var running = true;

        function drawFrame() {
          if (!running) return;
          try {
            if (hiddenVideo.videoWidth && hiddenVideo.videoHeight) {
              if (canvas.width !== hiddenVideo.videoWidth || canvas.height !== hiddenVideo.videoHeight) {
                canvas.width = hiddenVideo.videoWidth;
                canvas.height = hiddenVideo.videoHeight;
              }
            }
            var activeMode = getMode();
            ctx.filter = cssFilterForMode(activeMode);
            ctx.drawImage(hiddenVideo, 0, 0, canvas.width, canvas.height);
            ctx.filter = "none";
            if (activeMode === "orbit") drawOrbitOverlay(ctx, canvas.width, canvas.height);
          } catch (e) {}
          requestAnimationFrame(drawFrame);
        }

        drawFrame();

        if (!canvas.captureStream) return inputStream;

        var processedStream = canvas.captureStream(fps);
        var outputStream = new MediaStream();

        processedStream.getVideoTracks().forEach(function (track) {
          outputStream.addTrack(track);
          var originalStop = track.stop.bind(track);
          track.stop = function () {
            running = false;
            try { originalStop(); } catch (e) {}
            try { inputStream.getTracks().forEach(function (t) { t.stop(); }); } catch (e) {}
            try { hiddenVideo.remove(); } catch (e) {}
            try { canvas.remove(); } catch (e) {}
          };
        });

        inputStream.getAudioTracks().forEach(function (track) {
          outputStream.addTrack(track);
        });

        window.__AETHERIS_LAST_FILTERED_STREAM__ = outputStream;
        return outputStream;
      } catch (e) {
        return inputStream;
      }
    }

    if (!navigator.mediaDevices) navigator.mediaDevices = {};
    var originalGetUserMedia = navigator.mediaDevices.getUserMedia;
    if (!originalGetUserMedia) return;

    originalGetUserMedia = originalGetUserMedia.bind(navigator.mediaDevices);

    navigator.mediaDevices.getUserMedia = async function (constraints) {
      var stream = await originalGetUserMedia(constraints);
      try {
        var wantsVideo = constraints && constraints.video;
        if (!wantsVideo || !isFilterEnabled()) return stream;
        return await applyAetherisFilterToStream(stream);
      } catch (e) {
        return stream;
      }
    };

    window.__AETHERIS_WEBRTC_FILTER_STATUS__ = "installed";
  } catch (e) {}
})();
