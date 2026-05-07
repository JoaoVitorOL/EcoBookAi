package com.ecobook.material

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.SectionHeading

@Composable
fun MaterialUploadScreen(
    viewModel: MaterialUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it, ImageSource.GALLERY) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.onImageSelected(it, ImageSource.CAMERA) }
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

    LazyColumn(
        modifier = Modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeading(
                title = "Fluxo de doacao com IA",
                subtitle = "Agora o app ja envia a imagem real para /materiais/preview, deixa voce revisar os campos sugeridos e publica o material usando o upload temporario do backend."
            )
        }

        item {
            when (uiState.stage) {
                MaterialFlowStage.SELECT -> UploadSelectionContent(
                    uiState = uiState,
                    onChooseFromGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onCaptureWithCamera = {
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
                    onStartPreview = viewModel::startPreview,
                    onClearSelection = viewModel::clearSelectedImage
                )

                MaterialFlowStage.PROCESSING -> ProcessingScreen(selectedImage = uiState.selectedImage)

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

@Composable
private fun UploadSelectionContent(
    uiState: MaterialUploadUiState,
    onChooseFromGallery: () -> Unit,
    onCaptureWithCamera: () -> Unit,
    onStartPreview: () -> Unit,
    onClearSelection: () -> Unit
) {
    val context = LocalContext.current

    GlassCard {
        Text(
            text = "1. Escolha ou fotografe o material",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Voce pode selecionar uma imagem da galeria ou capturar uma nova foto com a camera. O app valida JPEG/PNG e comprime quando necessario para ficar abaixo de 5MB.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onChooseFromGallery, modifier = Modifier.fillMaxWidth()) {
                Text("Escolher da galeria")
            }
            OutlinedButton(onClick = onCaptureWithCamera, modifier = Modifier.fillMaxWidth()) {
                Text("Usar a camera")
            }
        }

        uiState.selectedImage?.let { image ->
            AsyncImage(
                model = image.uri,
                contentDescription = "Imagem selecionada para doacao",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
            )
            Text(
                text = image.fileName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Origem: ${if (image.source == ImageSource.CAMERA) "Camera" else "Galeria"} | ${if (image.mimeType.isBlank()) "Tipo a confirmar" else image.mimeType} | ${Formatter.formatShortFileSize(context, image.sizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStartPreview,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canStartPreview
                ) {
                    Text("Analisar com IA")
                }
                OutlinedButton(
                    onClick = onClearSelection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar")
                }
            }
        }

        uiState.backendMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

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
            text = uiState.backendMessage ?: "A publicacao foi concluida com sucesso.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        uiState.createdMaterial?.let { material ->
            Text(
                text = "Titulo: ${material.titulo}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Status: ${material.status} | Local: ${material.bairro}, ${material.cidade}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Button(onClick = onRestart) {
            Text("Publicar outro material")
        }
    }
}
