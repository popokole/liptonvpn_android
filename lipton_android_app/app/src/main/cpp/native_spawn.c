#include <jni.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <android/log.h>

#define TAG "LiptonNative"

/*
 * Spawns a child process without closing inherited file descriptors.
 * Android's ProcessBuilder closes all fd > 2 before exec(), which breaks
 * TUN fd inheritance to tun2socks. This JNI function uses fork()+execv()
 * directly, preserving all open descriptors (O_CLOEXEC-free ones survive exec).
 *
 * Returns int[2] = { pid, readPipeFd } on success, null on failure.
 * stdout+stderr of the child are merged into readPipeFd.
 */
JNIEXPORT jintArray JNICALL
Java_com_lipton_vpn_service_LiptonVpnService_nativeSpawn(
        JNIEnv *env, jobject thiz, jobjectArray jArgs) {

    int argc = (*env)->GetArrayLength(env, jArgs);
    if (argc == 0) return NULL;

    /* Copy all argument strings before fork() — JNI env is invalid in child */
    char **argv = (char **) malloc((argc + 1) * sizeof(char *));
    if (!argv) return NULL;

    for (int i = 0; i < argc; i++) {
        jstring js = (jstring) (*env)->GetObjectArrayElement(env, jArgs, i);
        const char *s = (*env)->GetStringUTFChars(env, js, NULL);
        argv[i] = strdup(s);
        (*env)->ReleaseStringUTFChars(env, js, s);
        (*env)->DeleteLocalRef(env, js);
    }
    argv[argc] = NULL;

    int pipefd[2];
    if (pipe(pipefd) != 0) {
        for (int i = 0; i < argc; i++) free(argv[i]);
        free(argv);
        return NULL;
    }

    pid_t pid = fork();

    if (pid < 0) {
        close(pipefd[0]);
        close(pipefd[1]);
        for (int i = 0; i < argc; i++) free(argv[i]);
        free(argv);
        return NULL;
    }

    if (pid == 0) {
        /* Child: redirect stdout+stderr → pipe write end */
        close(pipefd[0]);
        dup2(pipefd[1], STDOUT_FILENO);
        dup2(pipefd[1], STDERR_FILENO);
        if (pipefd[1] > STDERR_FILENO) close(pipefd[1]);

        /* exec without closing extra fds — TUN fd survives */
        execv(argv[0], argv);
        _exit(127);
    }

    /* Parent */
    close(pipefd[1]);
    for (int i = 0; i < argc; i++) free(argv[i]);
    free(argv);

    jintArray result = (*env)->NewIntArray(env, 2);
    if (result) {
        jint arr[2] = {(jint) pid, (jint) pipefd[0]};
        (*env)->SetIntArrayRegion(env, result, 0, 2, arr);
    } else {
        close(pipefd[0]);
        kill(pid, SIGTERM);
    }
    return result;
}

JNIEXPORT void JNICALL
Java_com_lipton_vpn_service_LiptonVpnService_nativeKill(
        JNIEnv *env, jobject thiz, jint pid) {
    if (pid > 0) {
        kill((pid_t) pid, SIGTERM);
        waitpid((pid_t) pid, NULL, WNOHANG);
    }
}
