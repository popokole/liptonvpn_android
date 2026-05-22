#include <jni.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
#include <android/log.h>

#define TAG "LiptonNative"

/*
 * Android's Os.dup() on API 26+ uses dup3(..., O_CLOEXEC), so the dup'd TUN fd
 * gets O_CLOEXEC set and is closed by the kernel on exec().
 *
 * Fix: after fork(), scan argv for "fd://N", check and clear O_CLOEXEC on that
 * fd with fcntl(F_SETFD, 0) before execv(). The fd then survives exec().
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

        /*
         * Scan args for "fd://N" and clear O_CLOEXEC on the TUN fd.
         * Android's Os.dup() uses dup3(..., O_CLOEXEC) internally, so the fd
         * arrives here with FD_CLOEXEC set and would be closed by exec().
         * fcntl(F_SETFD, flags & ~FD_CLOEXEC) makes it survive exec().
         */
        for (int i = 0; argv[i] != NULL; i++) {
            if (strncmp(argv[i], "fd://", 5) == 0) {
                int tun_fd = atoi(argv[i] + 5);
                if (tun_fd >= 0) {
                    int flags = fcntl(tun_fd, F_GETFD, 0);
                    char buf[160];
                    int n;
                    if (flags < 0) {
                        n = snprintf(buf, sizeof(buf),
                            "[native_spawn] ERROR: fd=%d invalid before exec (errno=%d)\n",
                            tun_fd, errno);
                        write(STDOUT_FILENO, buf, n);
                    } else {
                        /* Clear FD_CLOEXEC so the fd survives execv() */
                        fcntl(tun_fd, F_SETFD, flags & ~FD_CLOEXEC);
                        n = snprintf(buf, sizeof(buf),
                            "[native_spawn] fd=%d CLOEXEC was %s, cleared\n",
                            tun_fd, (flags & FD_CLOEXEC) ? "SET" : "NOT SET");
                        write(STDOUT_FILENO, buf, n);
                    }
                }
                break;
            }
        }

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
        kill((pid_t) pid, SIGKILL);
        /* Blocking wait — reaps the zombie so the kernel closes all its fds immediately.
           Called on Dispatchers.IO so blocking here is fine. */
        waitpid((pid_t) pid, NULL, 0);
    }
}
