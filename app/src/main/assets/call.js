// const l = require("./peerjs.js");
// const { Peer } = require("./peerjs.js");

let localVideo = document.getElementById("local-video");
let remoteVideo = document.getElementById("remote-video");

localVideo.style.opacity = 0;
remoteVideo.style.opacity = 0;

localVideo.onplaying = () => { localVideo.style.opacity = 1; };
remoteVideo.onplaying = () => { remoteVideo.style.opacity = 1; };

let peer;

function init(userId) {
    peer = new Peer(userId, {
        config: {
            'iceServers': [
                { url: 'stun:stun.l.google.com:19302' },
                { url: 'turn:numb.viagenie.ca:3478', credential: 'muazkh', username:'webrtc@live.com' },
                { url: 'turn:numb.viagenie.ca', credential: 'muazkh', username:'webrtc@live.com' },
                { url: 'turn:192.158.29.39:3478?transport=udp', credential: 'JZEOEt2V3Qb0y27GRntt2u2PAYA=', username:'28224511:1379330808' },
                { url: 'turn:192.158.29.39:3478?transport=tcp', credential: 'JZEOEt2V3Qb0y27GRntt2u2PAYA=', username:'28224511:1379330808' }
            ]
        },
        host: '35.240.136.28',
        port: 80,
        path: '/call'
    });

    peer.on('open', () => {
        Android.onPeerConnected()
    })

    listen();
}

let localStream;
function listen() {
    peer.on('call', (call) => {
        navigator.getUserMedia({
            audio: true,
            video: true
        }, (stream) => {
            localVideo.srcObject = stream;
            localStream = stream;

            call.answer(stream);
            call.on('stream', (remoteStream) => {
                remoteVideo.srcObject = remoteStream;
                remoteVideo.className = "primary-video";
                localVideo.className = "secondary-video";
            })
        })
    })
}

function startCall(otherUserId) {
    navigator.getUserMedia({
        audio: true,
        video:  true
    }, (stream) => {
        localVideo.srcObject = stream;
        localStream = stream;

        const call = peer.call(otherUserId, stream);
        call.on('stream', (remoteStream) => {
            remoteVideo.srcObject = remoteStream;
            remoteVideo.className = "primary-video";
            localVideo.className = "secondary-video";
        })
    })
}

function stopCall() {
    localVideo.pause();
    localVideo.currentTime = 0;
    remoteVideo.pause();
    remoteVideo.currentTime = 0;
}

function toggleVideo(b) {
    if(b == "true") {
        localStream.getVideoTracks()[0].enabled = true;
    } else {
        localStream.getVideoTracks()[0].enabled = false;
    }
}

function toggleAudio(b) {
    if(b == "true") {
        localStream.getAudioTracks()[0].enabled = true;
    } else {
        localStream.getAudioTracks()[0].enabled = false;
    }
}