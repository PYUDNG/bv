package dev.aaa1115910.biliapi.repositories

import bilibili.app.view.v1.ViewGrpcKt
import bilibili.app.view.v1.viewReq
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class VideoDetailRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository
) {
    private val viewStub
        get() = runCatching {
            ViewGrpcKt.ViewCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getVideoDetail(
        aid: Int,
        preferApiType: ApiType = ApiType.Web
    ): VideoDetail {
        return when (preferApiType) {
            ApiType.Web -> {
                withContext(Dispatchers.IO) {
                    val videoDetailWithoutUserActions = async {
                        val httpVideoDetail = BiliHttpApi.getVideoDetail(
                            av = aid,
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        VideoDetail.fromVideoDetail(httpVideoDetail)
                    }

                    //check liked, favoured, coined status...
                    val isFavoured = async {
                        runCatching {
                            favoriteRepository.checkVideoFavoured(
                                aid = aid,
                                preferApiType = ApiType.Web
                            )
                        }.onFailure {
                            println("Check video favoured failed: $it")
                        }.getOrDefault(false)
                    }

                    val history = async {
                        runCatching {
                            val videoModeInfo = BiliHttpApi.getVideoMoreInfo(
                                avid = aid,
                                cid = videoDetailWithoutUserActions.await().cid,
                                sessData = authRepository.sessionData ?: ""
                            ).getResponseData()
                            VideoDetail.History(
                                progress = videoModeInfo.lastPlayTime / 1000,
                                lastPlayedCid = videoModeInfo.lastPlayCid
                            )
                        }.onFailure {
                            println("Get video history failed: $it")
                        }.getOrDefault(VideoDetail.History(0, 0))
                    }

                    videoDetailWithoutUserActions.await().apply {
                        userActions.favorite = isFavoured.await()
                        this.history = history.await()
                    }
                }
            }

            ApiType.App -> {
                val viewReply = runCatching {
                    viewStub?.view(viewReq {
                        this.aid = aid.toLong()
                    }) ?: throw IllegalStateException("Player stub is not initialized")
                }.onFailure { handleGrpcException(it) }.getOrThrow()
                VideoDetail.fromViewReply(viewReply)
            }
        }
    }

    suspend fun getPgcVideoDetail(
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType = ApiType.Web
    ): SeasonDetail {
        when (preferApiType) {
            ApiType.Web -> {
                val webSeasonData = BiliHttpApi.getWebSeasonInfo(
                    epId = epid,
                    seasonId = seasonId,
                ).getResponseData()
                return SeasonDetail.fromSeasonData(webSeasonData)
            }

            ApiType.App -> {
                val appSeasonData = BiliHttpApi.getAppSeasonInfo(
                    epId = epid,
                    seasonId = seasonId,
                    mobiApp = "android_hd"
                ).getResponseData()
                return SeasonDetail.fromSeasonData(appSeasonData)
            }
        }
    }
}
