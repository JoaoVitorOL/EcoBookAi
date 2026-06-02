package com.ecobook.material

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.compose.foundation.layout.Spacer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ecobook.dto.MaterialDTO
import com.ecobook.ui.components.AdaptiveScreenContent
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading

@Composable
fun MaterialUploadScreen(
    modifier: Modifier = Modifier,
    viewModel: MaterialUploadViewModel = hiltViewModel(),
    topContent: (@Composable () -> Unit)? = null,
    showSectionHeading: Boolean = true,
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onMaterialPublished: (MaterialDTO) -> Unit = {},
    autoResetAfterPublish: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImageSlot by remember { mutableStateOf(ImageSlot.FRONT) }

    androidx.compose.runtime.LaunchedEffect(uiState.createdMaterial?.id) {
        if (autoResetAfterPublish) {
            uiState.createdMaterial?.let { material ->
                onMaterialPublished(material)
                viewModel.restartFlow()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it, ImageSource.GALLERY, pendingImageSlot) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.onImageSelected(it, ImageSource.CAMERA, pendingImageSlot) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = ImagePickerHelper.createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    AdaptiveScreenContent(modifier = modifier) {
        LazyColumn(
            modifier = it.imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        topContent?.let { content ->
            item { content() }
        }
        if (showSectionHeading) {
            item {
                SectionHeading(
                    title = "Fluxo de doação com IA",
                    subtitle = "Agora o app já envia a imagem real para /materiais/preview, deixa você revisar os campos sugeridos e publica o material usando o upload temporário do backend.",
                    trailingContent = {
                        com.ecobook.ui.components.NotificationsEntryPointButton(
                            unreadCount = unreadNotifications,
                            onClick = onOpenNotifications
                        )
                    }
                )
            }
        }

            item {
                when (uiState.stage) {
                MaterialFlowStage.SELECT -> UploadSelectionContent(
                    uiState = uiState,
                    onChooseFrontFromGallery = {
                        pendingImageSlot = ImageSlot.FRONT
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onCaptureFrontWithCamera = {
                        pendingImageSlot = ImageSlot.FRONT
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val uri = ImagePickerHelper.createCameraImageUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onClearFrontSelection = {
                        viewModel.clearSelectedImage(ImageSlot.FRONT)
                    },
                    onChooseBackFromGallery = {
                        pendingImageSlot = ImageSlot.BACK
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onCaptureBackWithCamera = {
                        pendingImageSlot = ImageSlot.BACK
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val uri = ImagePickerHelper.createCameraImageUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onClearBackSelection = {
                        viewModel.clearSelectedImage(ImageSlot.BACK)
                    },
                    onStartPreview = viewModel::startPreview,
                    onClearAllSelections = {
                        viewModel.clearSelectedImage()
                    }
                )

                MaterialFlowStage.PROCESSING -> ProcessingScreen(
                    selectedFrontImage = uiState.selectedFrontImage,
                    selectedBackImage = uiState.selectedBackImage
                )

                MaterialFlowStage.REVIEW -> ReviewScreen(
                    uiState = uiState,
                    onTituloChange = viewModel::updateTitulo,
                    onAutorChange = viewModel::updateAutor,
                    onEditoraChange = viewModel::updateEditora,
                    onDescricaoChange = viewModel::updateDescricao,
                    onAnoChange = viewModel::updateAno,
                    onDataPublicacaoChange = viewModel::updateDataPublicacao,
                    onDisciplinaChange = viewModel::updateDisciplina,
                    onNivelEnsinoChange = viewModel::updateNivelEnsino,
                    onSistemaEnsinoChange = viewModel::updateSistemaEnsino,
                    onEstadoConservacaoChange = viewModel::updateEstadoConservacao,
                    onNecessidadeAcademicaChange = viewModel::updateNecessidadeAcademica,
                    onPrepareConfirm = viewModel::prepareSubmit,
                    onRestart = viewModel::restartFlow,
                    onConfirm = viewModel::submitMaterial
                )

                MaterialFlowStage.SUCCESS -> SuccessContent(
                    uiState = uiState,
                    onRestart = viewModel::restartFlow
                )
            }
        }
    }
}
}

@Composable
private fun UploadSelectionContent(
    uiState: MaterialUploadUiState,
    onChooseFrontFromGallery: () -> Unit,
    onCaptureFrontWithCamera: () -> Unit,
    onClearFrontSelection: () -> Unit,
    onChooseBackFromGallery: () -> Unit,
    onCaptureBackWithCamera: () -> Unit,
    onClearBackSelection: () -> Unit,
    onStartPreview: () -> Unit,
    onClearAllSelections: () -> Unit
) {
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    GlassCard {
        Text(
            text = "1. Escolha ou fotografe o material",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Envie a capa da frente para a análise principal e, se quiser, adicione também a capa de trás. O app valida JPEG/PNG e comprime quando necessário para ficar abaixo de 5MB por imagem.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStartPreview,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canStartPreview
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analisar com IA")
            }
            if (uiState.selectedFrontImage != null || uiState.selectedBackImage != null) {
                FilledTonalButton(
                    onClick = { showClearAllConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = destructiveFilledTonalButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpar imagens")
                }
            }
        }

        ImageSelectionSlot(
            title = "Capa da frente",
            subtitle = "Obrigatória. É a imagem usada na análise inicial da IA.",
            selectedImage = uiState.selectedFrontImage,
            onChooseFromGallery = onChooseFrontFromGallery,
            onCaptureWithCamera = onCaptureFrontWithCamera,
            onClearSelection = onClearFrontSelection
        )

        ImageSelectionSlot(
            title = "Capa de trás",
            subtitle = "Opcional. Fica salva junto com o material para dar mais contexto a quem for receber.",
            selectedImage = uiState.selectedBackImage,
            onChooseFromGallery = onChooseBackFromGallery,
            onCaptureWithCamera = onCaptureBackWithCamera,
            onClearSelection = onClearBackSelection
        )

        uiState.backendMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = { Text("Limpar imagens") },
            text = {
                Text("Você vai remover as imagens selecionadas deste rascunho. A escolha precisará ser feita novamente para continuar.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirmation = false
                        onClearAllSelections()
                    }
                ) {
                    Text("Limpar tudo")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirmation = false }) {
                    Text("Manter imagens")
                }
            }
        )
    }
}

@Composable
private fun ImageSelectionSlot(
    title: String,
    subtitle: String,
    selectedImage: SelectedImageUiModel?,
    onChooseFromGallery: () -> Unit,
    onCaptureWithCamera: () -> Unit,
    onClearSelection: () -> Unit
) {
    val context = LocalContext.current
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        selectedImage?.let { image ->
            AsyncImage(
                model = image.uri,
                contentDescription = "Imagem selecionada para $title",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
            )
            Text(
                text = image.fileName,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Origem: ${if (image.source == ImageSource.CAMERA) "Câmera" else "Galeria"} | ${if (image.mimeType.isBlank()) "Tipo a confirmar" else image.mimeType} | ${Formatter.formatShortFileSize(context, image.sizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onChooseFromGallery, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escolher da galeria")
            }
            FilledTonalButton(onClick = onCaptureWithCamera, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usar a câmera")
            }
            if (selectedImage != null) {
                FilledTonalButton(
                    onClick = { showRemoveConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = destructiveFilledTonalButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remover imagem")
                }
            }
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("Remover imagem") },
            text = {
                Text("Esta imagem será removida do rascunho atual. Se precisar dela depois, você terá que selecioná-la novamente.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirmation = false
                        onClearSelection()
                    }
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveConfirmation = false }) {
                    Text("Manter imagem")
                }
            }
        )
    }
}

@Composable
private fun destructiveFilledTonalButtonColors() = ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
    disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f),
    disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.38f)
)

@Composable
private fun SuccessContent(
    uiState: MaterialUploadUiState,
    onRestart: () -> Unit
) {
    GlassCard {
        Text(
            text = "Material publicado",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = uiState.backendMessage ?: "A publicação foi concluída com sucesso.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        uiState.createdMaterial?.let { material ->
            Text(
                text = "Título: ${material.titulo}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Status: ${material.status} | Local: ${material.bairro}, ${material.cidade}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (material.imagemVersoUrl != null) {
                Text(
                    text = "As capas da frente e de trás foram salvas neste cadastro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(onClick = onRestart) {
            Text("Publicar outro material")
        }
    }
}
